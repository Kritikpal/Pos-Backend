package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.service.CsvService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Category;
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
        List<ItemRequest> itemRequests = new ArrayList<>();

        try (CSVReader csvReader = reader("products.csv", true)) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length > 1) {
                    String name = line[0];
                    String description = line[1];
                    double itemPrice = Double.parseDouble(line[2]);
                    String categoryName = line[3].trim();

                    Category category = categoryRepository.findByCategoryName(categoryName)
                            .orElse(new Category());

                    if (category.getCategoryId() == null) {
                        category.setCategoryName(categoryName);
                        category.setCategoryDescription("");
                        category = categoryRepository.save(category);
                    }

                    double discount = 0.0;
                    itemRequests.add(new ItemRequest(
                            null, name, description, itemPrice,
                            category.getCategoryId(), discount,
                            true, false, false, null, null
                    ));
                }
            }
        }

        return itemRequests;
    }

    @Override
    public List<CategoryRequest> readCategoriesFromCsv() throws AppException, CsvValidationException, IOException {
        List<CategoryRequest> categoryRequests = new ArrayList<>();

        try (CSVReader csvReader = reader("categories.csv", true)) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                String name = line[0];
                String description = line[1];
                categoryRequests.add(new CategoryRequest(null, null, name, description));
            }
        }

        return categoryRequests;
    }

    @Override
    public List<TableRequest> readTablesFromCsv() throws AppException, CsvValidationException, IOException {
        try (CSVReader csvReader = reader("tables.csv", true)) {
            CsvToBean<TableRequest> csvToBean = new CsvToBeanBuilder<TableRequest>(csvReader)
                    .withType(TableRequest.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csvToBean.parse();
        }
    }

    /** Helper to build a CSVReader (caller closes it via try-with-resources) */
    private CSVReader reader(String path, boolean hasHeader) throws AppException, CsvValidationException {
        try {
            InputStream inputStream = resourceLoader
                    .getResource("classpath:csv/" + path)
                    .getInputStream();

            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            CSVReader csvReader = new CSVReader(inputStreamReader);

            if (hasHeader) {
                csvReader.readNext(); // skip header row
            }

            return csvReader;
        } catch (IOException e) {
            throw new AppException("Unable to read the file " + path, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
