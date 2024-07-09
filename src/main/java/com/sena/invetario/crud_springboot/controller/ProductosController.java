package com.sena.invetario.crud_springboot.controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.List;


import com.sena.invetario.crud_springboot.models.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.sena.invetario.crud_springboot.models.ProductoDto;
import com.sena.invetario.crud_springboot.services.ProductosRepository;

import javax.validation.Valid;

@Controller
@RequestMapping("productos")
public class ProductosController {

    @Autowired
    private ProductosRepository repo;

    @GetMapping({ "", "/" })
    public String mostrarListaProductos(Model model) {
        List<Producto> productos = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("productos", productos);

        return "producto/index";
    }

    @GetMapping("/crear")
    public String showCreatePage(Model model) {
        ProductoDto productoDto = new ProductoDto();
        model.addAttribute("productoDto", productoDto);
        return "producto/crearproducto";
    }

    @PostMapping("/crear")
    public String crearproducto(@Valid @ModelAttribute ProductoDto productoDto, BindingResult resultado) {

        if (productoDto.getArchivoImage().isEmpty()) {
            resultado.addError(
                    new FieldError("productoDto", "archivoImage", "El archivo para la imagen es obligatorio"));
        }

        if (resultado.hasErrors()) {
            return "producto/crearproducto";
        }

        // Grabar Archivo de Imagen
        MultipartFile image = productoDto.getArchivoImage();
        Date fechaCreacion = new Date(System.currentTimeMillis());
        String storageFileName = fechaCreacion.getTime() + "_" + image.getOriginalFilename();

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception ex) {
            System.out.println("Exepción al grabar: " + ex.getMessage());
        }

        // Registro en base de datos del nuevo registro
        Producto prod = new Producto();
        prod.setNombre(productoDto.getNombre());
        prod.setCategoria(productoDto.getCategoria());
        prod.setCosto(productoDto.getCosto());
        prod.setDescripcion(productoDto.getDescripcion());
        prod.setFechaCreacion((java.sql.Date) fechaCreacion);
        prod.setNombreArchivoImagen(storageFileName);

        repo.save(prod);

        return "redirect:/productos";
    }

    @GetMapping("/editar")
    public String showEditPag(Model model, @RequestParam int id) {
        try {
            Producto prod = repo.findById(id).get();
            model.addAttribute("producto", prod);

            ProductoDto productoDto = new ProductoDto();
            productoDto.setNombre(prod.getNombre());
            productoDto.setCategoria(prod.getCategoria());
            productoDto.setCosto(prod.getCosto());
            productoDto.setDescripcion(prod.getDescripcion());

            model.addAttribute("productoDto", productoDto);
        } catch (Exception ex) {
            System.out.println("Excepción al Editar: " + ex.getMessage());
        }

        return "producto/editarproducto";
    }

    @PostMapping("/editar")
    public String actualizarProducto(Model model, @RequestParam int id, @Valid @ModelAttribute ProductoDto productoDto,
                                     BindingResult resultado) {

        try {
            Producto producto = repo.findById(id).get();
            model.addAttribute("productos", producto);
            // Si no hay errore
            if (resultado.hasErrors()) {
                return "producto/editarproducto";
            }
            //
            if (!productoDto.getArchivoImage().isEmpty()) {
                // Eliminamos la imagen antigua
                String dirDeImagenes = "public/images/";
                Path rutaAntiguaImagen = Paths.get(dirDeImagenes + producto.getNombreArchivoImagen());

                try {
                    Files.delete(rutaAntiguaImagen);
                } catch (Exception ex) {
                    System.out.println("Excepción: " + ex.getMessage());
                }

                // Grabar el archivo de la nueva imagen
                MultipartFile image = productoDto.getArchivoImage();
                Date fechaCreacion = new Date(System.currentTimeMillis());
                String storageFileName = fechaCreacion.getTime() + "_" + image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, Paths.get(dirDeImagenes + storageFileName),
                            StandardCopyOption.REPLACE_EXISTING);
                }

                producto.setNombreArchivoImagen(storageFileName);

            }

            producto.setNombre(productoDto.getNombre());
            producto.setCategoria(productoDto.getCategoria());
            producto.setCosto(productoDto.getCosto());
            producto.setDescripcion(productoDto.getDescripcion());

            repo.save(producto);

        } catch (Exception ex) {
            System.out.println("Excepción al grabar la edicón: " + ex.getMessage());
        }

        return "redirect:/productos";
    }

    @GetMapping("/eliminar")
    public String eliminarProducto(@RequestParam int id) {

        try {
            Producto producto = repo.findById(id).get();
            // Eliminamos la imagen de la lista de producto
            Path rutaImagen = Paths.get("public/images/" + producto.getNombreArchivoImagen());
            try {
                Files.delete(rutaImagen);
            } catch (Exception ex) {
                System.out.println("Excepcion al Eliminar: " + ex.getMessage());
            }

            // Eliminar el producto de la base de datos
            repo.delete(producto);

        } catch (Exception ex) {
            System.out.println("Excepcion al Eliminar: " + ex.getMessage());
        }

        return "redirect:/productos";
    }

}
