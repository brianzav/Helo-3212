package br.com.senac.gamerx.controller;

import br.com.senac.gamerx.model.ProductImages;
import br.com.senac.gamerx.model.ProductModel;
import br.com.senac.gamerx.model.UserModel;
import br.com.senac.gamerx.repository.ProductRepository;
import br.com.senac.gamerx.repository.UserRepository;
import br.com.senac.gamerx.service.HashingService;
import br.com.senac.gamerx.service.StorageService;
import br.com.senac.gamerx.utils.GamerXUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final HashingService hashingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    public AdminController(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<UserModel> users = userRepository.findAll();
        model.addAttribute("users", users.stream().map(GamerXUtils::convertModelToUserDTO).collect(Collectors.toList()));
        return "listUsers";
    }

    @PostMapping("/users/update")
    public String updateUser(@ModelAttribute UserModel user, RedirectAttributes redirectAttributes) {
        UserModel existingUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com email: " + user.getEmail()));
        user.setActive(existingUser.isActive());

        if (!user.getPassword().isEmpty()) {
            String hashedPassword = hashingService.hashPassword(user.getPassword());
            user.setPassword(hashedPassword);
        } else {
            user.setPassword(existingUser.getPassword());
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Usuário atualizado com sucesso!");
        return "redirect:/admin/users"; // Redireciona para a lista de usuários
    }

    @GetMapping("/users/edit/{email}")
    public String editUser(@PathVariable String email, Model model) {
        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com email: " + email));
        model.addAttribute("user", user);
        return "editUser";
    }

    @GetMapping("/products")
    public String listProducts(Model model, @RequestParam(defaultValue = "0") int page) {
        Page<ProductModel> productPage = productRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 10));
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        return "listProducts";
    }

    @PostMapping("/products")
    public String saveProduct(@ModelAttribute ProductModel product, @RequestParam("images") MultipartFile[] images, RedirectAttributes redirectAttributes) {
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                String filename = StringUtils.cleanPath(image.getOriginalFilename());
                storageService.store(image, filename);
                ProductImages productImage = new ProductImages();
                productImage.setImagePath("/assets/" + filename);
                product.getProductImages().add(productImage);
                productImage.setProduct(product);
            }
        }

        productRepository.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Produto adicionado com sucesso!");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/toggle-status/{id}")
    public String toggleProductStatus(@PathVariable("id") Long productId, RedirectAttributes redirectAttributes) {
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido: " + productId));

        product.setActive(!product.isActive());
        productRepository.save(product);

        String message = product.isActive() ? "Produto ativado com sucesso!" : "Produto desativado com sucesso!";
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/products";
    }

    @GetMapping("products/edit/{id}")
    public String editProduct(@PathVariable("id") Long id, Model model) {
        ProductModel product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + id));
        model.addAttribute("product", product);
        return "editProduct";
    }

    @PostMapping("products/update")
    public String updateProduct(@ModelAttribute ProductModel product, RedirectAttributes redirectAttributes) {
        ProductModel existingProduct = productRepository.findById(product.getProductID())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + product.getProductID()));

        existingProduct.setProductName(product.getProductName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStorage(product.getStorage());
        existingProduct.setDescription(product.getDescription());

        productRepository.save(existingProduct);
        redirectAttributes.addFlashAttribute("successMessage", "Produto atualizado com sucesso!");

        return "redirect:/admin/products";
    }

    @GetMapping("/products/view/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        ProductModel product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
        model.addAttribute("product", product);
        model.addAttribute("images", product.getProductImages());
        return "telaProd";
    }

}

