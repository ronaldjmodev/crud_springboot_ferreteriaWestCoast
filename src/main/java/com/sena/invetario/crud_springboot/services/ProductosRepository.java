package com.sena.invetario.crud_springboot.services;

import com.sena.invetario.crud_springboot.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductosRepository extends JpaRepository<Producto,Integer> {

}