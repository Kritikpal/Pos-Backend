package com.kritik.POS.common.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.util.List;

public interface CsvService {
     List<ItemRequest> readProductsFromCsv() throws AppException, CsvValidationException, IOException;
     List<CategoryRequest> readCategoriesFromCsv() throws AppException, CsvValidationException, IOException;
     List<TableRequest> readTablesFromCsv() throws AppException, CsvValidationException;

}
