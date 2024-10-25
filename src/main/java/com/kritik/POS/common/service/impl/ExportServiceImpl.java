package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.service.ExportService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.DAO.Category;
import com.kritik.POS.restaurant.DAO.MenuItem;
import com.kritik.POS.restaurant.DAO.RestaurantTable;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private final ResourceLoader resourceLoader;

    @Autowired
    public ExportServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void writeProductsToCsv(List<MenuItem> items) throws AppException {
        String[] headers =  new String[]{"itemName", "description", "itemPrice", "categoryId"};
        try {
            Writer writer = new FileWriter("src/main/resources/csv/products.csv" , StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(writer);
            // Writing the header
            csvWriter.writeNext(headers);

            // Writing the products
            for (MenuItem item : items) {
                String[] itemData = new String[]{
                        item.getItemName(),
                        item.getDescription(),
                        String.valueOf(item.getItemPrice().getPrice()),
                        String.valueOf(item.getCategory().getCategoryName())
                };
                csvWriter.writeNext(itemData);
            }
            csvWriter.close();
        } catch (IOException e) {
            throw new AppException("Unable to write to the file : products.csv", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void writeCategoriesToCsv(List<Category> categories) throws AppException {
        try {
            Writer writer = new FileWriter("src/main/resources/csv/categories.csv" , StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(writer);

            // Writing the header
            csvWriter.writeNext(new String[]{"categoryName", "categoryDescription"});

            // Writing the categories
            for (Category category : categories) {
                String[] categoryData = new String[]{
                        category.getCategoryName(),
                        category.getCategoryDescription()
                };
                csvWriter.writeNext(categoryData);
            }

            csvWriter.close();
        } catch (IOException e) {
            throw new AppException("Unable to write to the file: categories.csv", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void writeTablesToCsv(List<RestaurantTable> tables) throws AppException {
        try {
            Writer writer = new FileWriter("src/main/resources/csv/tables.csv", StandardCharsets.UTF_8);
            CSVWriter csvWriter = new CSVWriter(writer);

            // Writing the header
            csvWriter.writeNext(new String[]{"tableId", "tableNumber", "capacity"});

            // Writing the tables
            for (RestaurantTable table : tables) {
                String[] tableData = new String[]{
                        String.valueOf(table.getTableId()),
                        String.valueOf(table.getTableNumber()),
                        String.valueOf(table.getSeats())
                };
                csvWriter.writeNext(tableData);
            }

            csvWriter.close();
        } catch (IOException e) {
            throw new AppException("Unable to write to the file: tables.csv" , HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
