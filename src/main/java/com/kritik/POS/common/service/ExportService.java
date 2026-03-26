package com.kritik.POS.common.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.RestaurantTable;

import java.util.List;

public interface ExportService {

    void writeProductsToCsv(List<MenuItem> items) throws AppException;

    void writeCategoriesToCsv(List<Category> categories) throws AppException;

    void writeTablesToCsv(List<RestaurantTable> tables) throws AppException;
}
