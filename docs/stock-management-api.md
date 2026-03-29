# Stock Management API

All endpoints return the existing response envelope:

```json
{
  "success": true,
  "data": {},
  "responseCode": "SUCCESS",
  "message": "Success"
}
```

All endpoints below require authentication.

## 1. Product APIs changed

### `POST /menuEdit`
Multipart endpoint used to create or update a product.

Parts:

- `itemRequest` as JSON
- `productImage` as optional file

Updated `itemRequest` shape:

```json
{
  "itemId": 12,
  "itemName": "Masala Dosa",
  "description": "Crisp dosa with potato filling",
  "itemPrice": 149,
  "categoryId": 4,
  "supplierId": 3,
  "disCount": 10,
  "isActive": true,
  "isAvailable": true,
  "isTrending": false,
  "totalStocks": 40,
  "reorderLevel": 10,
  "unitOfMeasure": "plate"
}
```

Notes:

- `categoryId` is the product category selector for the frontend.
- `supplierId`, `reorderLevel`, and `unitOfMeasure` are new optional stock-management fields.
- Validation now rejects blank names/descriptions and negative price/stock values.

Updated product detail response now includes stock metadata:

```json
{
  "id": 12,
  "sku": "d2c9e4d5-8f3b-4a8d-bfd4-77d7d3d4c621",
  "itemName": "Masala Dosa",
  "description": "Crisp dosa with potato filling",
  "itemPrice": {
    "price": 149.0,
    "disCountedPrice": 134.1,
    "disCount": 10.0
  },
  "isAvailable": true,
  "isActive": true,
  "isTrending": false,
  "itemInStock": 40,
  "reorderLevel": 10,
  "unitOfMeasure": "plate",
  "lowStock": false,
  "category": {
    "categoryId": 4,
    "categoryName": "South Indian",
    "categoryDescription": "Breakfast items",
    "isActive": true
  },
  "supplier": {
    "supplierId": 3,
    "supplierName": "Fresh Foods Supply",
    "contactPerson": "Ravi Kumar"
  }
}
```

### `GET /api/restaurants/menu-items`
Paginated product list response now also includes:

- `sku`
- `reorderLevel`
- `unitOfMeasure`
- `lowStock`
- `supplierId`
- `supplierName`

### Category APIs for product form

Use these existing endpoints to populate the product category dropdown:

- `GET /getAllCategories`
- `GET /api/restaurants/categories`

## 2. Stock APIs added

### `GET /api/stock-management/stocks`
Paginated stock list.

Query params:

- `chainId` optional
- `restaurantId` optional
- `isActive` optional
- `lowStockOnly` optional, default `false`
- `search` optional
- `page` default `0`
- `size` default `20`

Stock row shape:

```json
{
  "sku": "d2c9e4d5-8f3b-4a8d-bfd4-77d7d3d4c621",
  "restaurantId": 2,
  "menuItemId": 12,
  "itemName": "Masala Dosa",
  "categoryId": 4,
  "categoryName": "South Indian",
  "totalStock": 40,
  "reorderLevel": 10,
  "unitOfMeasure": "plate",
  "lowStock": false,
  "supplierId": 3,
  "supplierName": "Fresh Foods Supply",
  "isActive": true,
  "isAvailable": true,
  "lastRestockedAt": "2026-03-28T20:30:00",
  "updatedAt": "2026-03-28T20:30:00"
}
```

### `GET /api/stock-management/stocks/{sku}`
Single stock detail for a product SKU.

### `PUT /api/stock-management/stocks/{sku}`
Update stock settings or manually correct stock quantity.

Request body:

```json
{
  "totalStock": 55,
  "reorderLevel": 12,
  "unitOfMeasure": "plate",
  "supplierId": 3,
  "isActive": true
}
```

Notes:

- If `totalStock` is set to `0`, the linked product becomes unavailable.
- If `totalStock` becomes positive and stock is active, the linked product becomes available again.

## 3. Supplier APIs added

### `GET /api/stock-management/suppliers`
Non-paginated supplier list for dropdowns.

Query params:

- `chainId` optional
- `restaurantId` optional
- `isActive` optional

### `GET /api/stock-management/suppliers/page`
Paginated supplier list.

Query params:

- `chainId` optional
- `restaurantId` optional
- `isActive` optional
- `search` optional
- `page` default `0`
- `size` default `20`

### `GET /api/stock-management/suppliers/{id}`
Single supplier detail.

### `POST /api/stock-management/suppliers`
Create or update supplier.

Request body:

```json
{
  "supplierId": null,
  "restaurantId": 2,
  "supplierName": "Fresh Foods Supply",
  "contactPerson": "Ravi Kumar",
  "phoneNumber": "+91 9876543210",
  "email": "ravi@freshfoods.example",
  "address": "12 Market Road, Bengaluru",
  "taxIdentifier": "GSTIN123456789",
  "notes": "Morning delivery preferred",
  "isActive": true
}
```

Validation highlights:

- `supplierName` is required
- `email` must be valid if sent
- `phoneNumber` must be 7 to 20 chars if sent

### `DELETE /api/stock-management/suppliers/{id}`
Soft-deletes the supplier.

## 4. Stock receipt APIs added

### `GET /api/stock-management/receipts`
Paginated stock receipt list.

Query params:

- `chainId` optional
- `restaurantId` optional
- `search` optional
- `page` default `0`
- `size` default `20`

### `GET /api/stock-management/receipts/{id}`
Single receipt with its line items.

### `POST /api/stock-management/receipts`
Creates a stock receipt and increases inventory for all listed SKUs.

Request body:

```json
{
  "restaurantId": 2,
  "supplierId": 3,
  "invoiceNumber": "INV-2026-0091",
  "receivedAt": "2026-03-28T19:30:00",
  "notes": "Received in good condition",
  "items": [
    {
      "sku": "d2c9e4d5-8f3b-4a8d-bfd4-77d7d3d4c621",
      "quantityReceived": 20,
      "unitCost": 95
    },
    {
      "sku": "f0c5f4ba-8d1f-4d80-9278-6648e54cb1fb",
      "quantityReceived": 15,
      "unitCost": 60
    }
  ]
}
```

Important behavior:

- All receipt items must belong to the same restaurant as the supplier.
- Posting a receipt updates stock quantities immediately.
- Supplier on the stock record is also synced from the receipt.

Receipt response shape:

```json
{
  "receiptId": 21,
  "receiptNumber": "REC-7D91A2BC",
  "restaurantId": 2,
  "supplier": {
    "supplierId": 3,
    "restaurantId": 2,
    "supplierName": "Fresh Foods Supply",
    "contactPerson": "Ravi Kumar",
    "phoneNumber": "+91 9876543210",
    "email": "ravi@freshfoods.example",
    "address": "12 Market Road, Bengaluru",
    "taxIdentifier": "GSTIN123456789",
    "notes": "Morning delivery preferred",
    "isActive": true
  },
  "invoiceNumber": "INV-2026-0091",
  "receivedAt": "2026-03-28T19:30:00",
  "totalItems": 2,
  "totalQuantity": 35,
  "totalCost": 2800.0,
  "notes": "Received in good condition",
  "items": [
    {
      "receiptItemId": 100,
      "sku": "d2c9e4d5-8f3b-4a8d-bfd4-77d7d3d4c621",
      "menuItemId": 12,
      "menuItemName": "Masala Dosa",
      "categoryId": 4,
      "categoryName": "South Indian",
      "quantityReceived": 20,
      "unitCost": 95.0,
      "totalCost": 1900.0
    }
  ]
}
```

## 5. Frontend implementation order

Recommended order for frontend wiring:

1. Load categories from `GET /getAllCategories` or `GET /api/restaurants/categories` for the product form.
2. Load suppliers from `GET /api/stock-management/suppliers` for supplier dropdowns.
3. Use `POST /menuEdit` for product create/edit.
4. Use `GET /api/stock-management/stocks` for stock listing and `PUT /api/stock-management/stocks/{sku}` for manual stock corrections.
5. Use `POST /api/stock-management/receipts` when stock is received from a supplier.
