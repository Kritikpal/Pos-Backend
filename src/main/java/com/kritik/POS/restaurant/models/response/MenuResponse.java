package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.DAO.ItemPrice;
import com.kritik.POS.restaurant.DAO.ItemStock;
import com.kritik.POS.restaurant.DAO.MenuItem;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MenuResponse {
    private Long id;
    private String itemName;
    private String description;
    private MenuItemPrice itemPrice;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Boolean isTrending;
    private CategoryResponse category;
    private Integer itemInStock;

    @Data
    public static class MenuItemPrice {
        private Double price;
        private Double disCountedPrice;
        private Double disCount;

        public MenuItemPrice(ItemPrice itemPrice) {
            this.price = itemPrice.getPrice();
            this.disCount = itemPrice.getDisCount();
            this.disCountedPrice = RestaurantUtil.getMenuItemPrice(itemPrice);
        }
    }

    public static MenuResponse buildResponseFromMenu(MenuItem menuItem) {
        MenuResponse menuResponse = new MenuResponse();
        menuResponse.setId(menuItem.getId());
        menuResponse.setItemName(menuItem.getItemName());
        menuResponse.setDescription(menuItem.getDescription());
        menuResponse.setItemPrice(new MenuItemPrice(menuItem.getItemPrice()));
        menuResponse.setIsAvailable(menuItem.getIsAvailable());
        menuResponse.setCreatedAt(menuItem.getCreatedAt());
        menuResponse.setIsActive(menuItem.getIsActive());
        menuResponse.setIsTrending(menuItem.getIsTrending());
        menuResponse.setCategory(new CategoryResponse(menuItem.getCategory().getCategoryId(),
                menuItem.getCategory().getCategoryName(),
                menuItem.getCategory().getCategoryDescription(),
                menuItem.getCategory().getIsActive()));
        ItemStock itemStock = menuItem.getItemStock();
        menuResponse.setItemInStock(itemStock.getTotalStock());
        return menuResponse;
    }


}
