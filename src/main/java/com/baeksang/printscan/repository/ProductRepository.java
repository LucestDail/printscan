package com.baeksang.printscan.repository;

import com.baeksang.printscan.model.Product;
import org.apache.ibatis.annotations.Mapper;
import java.util.UUID;
import java.util.List;

@Mapper
public interface ProductRepository {
    void save(Product product);
    List<Product> findAll();
    Product findById(String id);
    void deleteById(String id);
}
