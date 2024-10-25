package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.service.CsvService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.DAO.Category;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CsvServiceImpl implements CsvService {
    private final ResourceLoader resourceLoader;
    private final CategoryRepository categoryRepository;

    @Autowired
    public CsvServiceImpl(ResourceLoader resourceLoader, CategoryRepository categoryRepository) {
        this.resourceLoader = resourceLoader;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<ItemRequest> readProductsFromCsv() throws AppException, CsvValidationException, IOException {
        CSVReader csvReader = reader("products.csv", true);
        csvReader.readNext();
        String[] line;
        List<ItemRequest> categoryRequestList = new ArrayList<>();
        while ((line = csvReader.readNext()) != null) {
            if (line.length > 1) {
                String name = line[0];
                String description = line[1];
                double itemPrice = Double.parseDouble(line[2]);
                String categoryName = String.valueOf(line[3]).trim();
                Category category = categoryRepository.findByCategoryName(categoryName).orElse(new Category());
                if (category.getCategoryId()==null){
                    category.setCategoryName(categoryName);
                    category.setCategoryDescription("");
                    category = categoryRepository.save(category);
                }
                double discount = 0.0;
                categoryRequestList.add(new ItemRequest(null, name,
                        description, itemPrice, category.getCategoryId(), discount,
                        true,true,false,100));
            }

        }
        return categoryRequestList;
    }

    @Override
    public List<CategoryRequest> readCategoriesFromCsv() throws AppException, CsvValidationException, IOException {
        CSVReader csvReader = reader("categories.csv", true);
        String[] line;
        List<CategoryRequest> categoryRequestList = new ArrayList<>();
        while ((line = csvReader.readNext()) != null) {
            String name = line[0];
            String description = line[1];
            categoryRequestList.add(new CategoryRequest(null, name, description));
        }
        return categoryRequestList;
    }


    @Override
    public List<TableRequest> readTablesFromCsv() throws AppException, CsvValidationException {
        CSVReader reader = reader("tables.csv", true);
        CsvToBean<TableRequest> csvToBean = new CsvToBeanBuilder<TableRequest>(reader)
                .withType(TableRequest.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();
        return csvToBean.parse();
    }

    private CSVReader reader(String path, boolean hasHeader) throws AppException, CsvValidationException {
        try {
            InputStream inputStream = resourceLoader.getResource("classpath:csv/" + path).getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReader(inputStreamReader);
            if (hasHeader) {
                csvReader.readNext();
            }
            return csvReader;
        } catch (IOException e) {
            throw new AppException("Unable to read the file" + path, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
