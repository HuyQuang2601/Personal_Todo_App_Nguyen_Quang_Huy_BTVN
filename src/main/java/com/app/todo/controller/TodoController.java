package com.app.todo.controller;

import com.app.todo.model.Todo;
import com.app.todo.model.TodoPriority;
import com.app.todo.model.TodoStatus;
import com.app.todo.repository.TodoRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/todos")
public class TodoController {
    private static final String OWNER_NAME_SESSION_KEY = "ownerName";
    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping
    public String showTodos(Model model, HttpSession session) {
        Object ownerName = session.getAttribute(OWNER_NAME_SESSION_KEY);
        if (ownerName == null || ownerName.toString().isBlank()) {
            return "todos/owner";
        }

        if (!model.containsAttribute("todo")) {
            model.addAttribute("todo", new Todo());
        }

        model.addAttribute("ownerName", ownerName);
        populateFormOptions(model);
        model.addAttribute("todos", todoRepository.findAll());
        return "todos/index";
    }

    @PostMapping("/owner")
    public String saveOwnerName(
            @RequestParam(value = "ownerName", required = false) String ownerName,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("messageKey", "owner.message.required");
            return "redirect:/todos";
        }

        session.setAttribute(OWNER_NAME_SESSION_KEY, ownerName.trim());
        redirectAttributes.addFlashAttribute("messageKey", "owner.message.saved");
        return "redirect:/todos";
    }

    @PostMapping("/save")
    public String saveTodo(
            @Valid @ModelAttribute("todo") Todo todo,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        model.addAttribute("ownerName", session.getAttribute(OWNER_NAME_SESSION_KEY));
        if (bindingResult.hasErrors()) {
            populateFormOptions(model);
            model.addAttribute("todos", todoRepository.findAll());
            return "todos/index";
        }

        boolean isUpdate = todo.getId() != null;
        todoRepository.save(todo);
        redirectAttributes.addFlashAttribute("messageKey",
                isUpdate ? "todo.message.updated" : "todo.message.created");
        return "redirect:/todos";
    }

    @GetMapping("/edit/{id}")
    public String editTodo(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        return todoRepository.findById(id)
                .map(todo -> {
                    model.addAttribute("todo", todo);
                    model.addAttribute("ownerName", session.getAttribute(OWNER_NAME_SESSION_KEY));
                    populateFormOptions(model);
                    model.addAttribute("todos", todoRepository.findAll());
                    return "todos/index";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("messageKey", "todo.message.notFoundForEdit");
                    return "redirect:/todos";
                });
    }

    @GetMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!todoRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("messageKey", "todo.message.notFoundForDelete");
            return "redirect:/todos";
        }

        todoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("messageKey", "todo.message.deleted");
        return "redirect:/todos";
    }

    @ModelAttribute("statuses")
    public TodoStatus[] statuses() {
        return TodoStatus.values();
    }

    @ModelAttribute("priorities")
    public TodoPriority[] priorities() {
        return TodoPriority.values();
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("statuses", statuses());
        model.addAttribute("priorities", priorities());
    }
}
