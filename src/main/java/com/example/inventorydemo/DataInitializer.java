package com.example.inventorydemo;

import com.example.inventorydemo.product.Product;
import com.example.inventorydemo.product.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        // 初始化一些示例商品数据
        if (productRepository.count() == 0) {
            Product product1 = new Product();
            product1.setCode("P001");
            product1.setName("苹果");
            product1.setDescription("新鲜红富士苹果");
            product1.setPrice(new BigDecimal("5.99"));
            productRepository.save(product1);

            Product product2 = new Product();
            product2.setCode("P002");
            product2.setName("香蕉");
            product2.setDescription("进口香蕉");
            product2.setPrice(new BigDecimal("3.99"));
            productRepository.save(product2);

            Product product3 = new Product();
            product3.setCode("P003");
            product3.setName("橙子");
            product3.setDescription("赣南脐橙");
            product3.setPrice(new BigDecimal("6.99"));
            productRepository.save(product3);

            log.info("示例商品数据已初始化");
        }
    }
}
