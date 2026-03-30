# Frontend Prompt: Product and Inventory API Refactor

Use this prompt for the Next.js frontend refactor.

## Prompt

You are updating the frontend for a backend refactor that separates product management from inventory management.

### Goal

Refactor the product create/edit flow so menu items are managed from the restaurant/product APIs, and stock/inventory is managed from the new inventory APIs.

### Important Behavioral Change

Creating or updating a product no longer creates or updates `itemStock`.

The frontend must now use a two-step flow for non-recipe products:

1. Create or update the product through the restaurant API.
2. Create or update its stock through the inventory API.

Recipe-based products must not create item stock. Their availability is driven by ingredient inventory only.

### Endpoint Changes

#### Product APIs stay under restaurant service

Use the existing product endpoints for menu items:

- `GET /getAllItems`
- `GET /api/restaurants/menu-items`
- `GET /getMenuItem/{id}`
- `POST /menuEdit` with `multipart/form-data`
- `DELETE /deleteMenuItem/{id}`

#### Inventory APIs moved to new base path

The old inventory base path:

- `"/api/stock-management"`

has been replaced with:

- `"/api/inventory"`

Use these inventory endpoints now:

- `GET /api/inventory/stocks`
- `POST /api/inventory/stocks`
- `GET /api/inventory/stocks/{sku}`
- `PUT /api/inventory/stocks/{sku}`
- `GET /api/inventory/ingredients`
- `GET /api/inventory/ingredients/{sku}`
- `POST /api/inventory/ingredients`
- `DELETE /api/inventory/ingredients/{sku}`
- `GET /api/inventory/suppliers`
- `GET /api/inventory/suppliers/page`
- `GET /api/inventory/suppliers/{id}`
- `POST /api/inventory/suppliers`
- `DELETE /api/inventory/suppliers/{id}`
- `GET /api/inventory/receipts`
- `GET /api/inventory/receipts/{id}`
- `POST /api/inventory/receipts`

### Product Request DTO Change

The `itemRequest` payload sent to `POST /menuEdit` has changed.

#### Old fields removed from `ItemRequest`

Remove these fields from the product form payload:

- `supplierId`
- `totalStocks`
- `reorderLevel`
- `unitOfMeasure`

#### Current `ItemRequest`

```ts
type ItemRequest = {
  itemId?: number | null
  itemName: string
  description: string
  itemPrice: number
  categoryId: number
  disCount?: number | null
  isActive?: boolean | null
  isAvailable?: boolean | null
  isTrending?: boolean | null
  ingredients?: Array<{
    ingredientSku: string
    quantityRequired: number
  }> | null
}
```

#### Product request example

```json
{
  "itemId": null,
  "itemName": "Veg Burger",
  "description": "House burger with lettuce and sauce",
  "itemPrice": 149,
  "categoryId": 12,
  "disCount": 10,
  "isActive": true,
  "isAvailable": false,
  "isTrending": false,
  "ingredients": []
}
```

### New Inventory Stock Create API

After product creation, create stock separately for non-recipe products.

#### `POST /api/inventory/stocks`

```ts
type ItemStockUpsertRequest = {
  menuItemId: number
  totalStock: number
  reorderLevel?: number | null
  unitOfMeasure?: string | null
  supplierId?: number | null
  isActive?: boolean | null
}
```

#### Example request

```json
{
  "menuItemId": 101,
  "totalStock": 50,
  "reorderLevel": 10,
  "unitOfMeasure": "unit",
  "supplierId": 7,
  "isActive": true
}
```

#### Example response shape

```ts
type StockResponse = {
  sku: string
  restaurantId: number
  menuItemId: number
  itemName: string
  description: string
  totalStock: number
  reorderLevel: number
  unitOfMeasure: string
  lowStock: boolean
  isActive: boolean
  isAvailable: boolean
  lastRestockedAt: string | null
  createdAt: string | null
  updatedAt: string | null
  supplier?: {
    supplierId: number
    supplierName: string
    contactPerson?: string | null
  } | null
  category?: {
    categoryId: number
    categoryName: string
    categoryDescription?: string | null
    isActive?: boolean | null
  } | null
}
```

### Existing Inventory Stock Update API

Use this when a stock record already exists:

- `PUT /api/inventory/stocks/{sku}`

Request body:

```ts
type StockUpdateRequest = {
  totalStock?: number | null
  reorderLevel?: number | null
  unitOfMeasure?: string | null
  supplierId?: number | null
  isActive?: boolean | null
}
```

### Menu Response Impact

`MenuResponse` can still contain stock-related fields like:

- `sku`
- `itemInStock`
- `reorderLevel`
- `unitOfMeasure`
- `lowStock`
- `supplier`

But those are now read-only inventory views. Do not send them back in the product create/edit request.

For newly created non-recipe products, expect these values to be empty until stock is created through inventory APIs.

### Frontend Workflow Rules

#### For non-recipe products

1. Submit product form to `POST /menuEdit`.
2. Read `id` from the returned `MenuResponse`.
3. If the UI collected stock data, immediately call `POST /api/inventory/stocks`.
4. On edit:
   - update product fields through restaurant API
   - update stock fields through inventory API

#### For recipe-based products

1. Submit product form with `ingredients`.
2. Do not call `POST /api/inventory/stocks`.
3. Manage availability through ingredient inventory only.

### UI Requirements

- Split the existing product form into:
  - product details
  - inventory details
- Do not mix inventory payload into the product submit body.
- If the product is recipe-based, hide or disable item stock fields.
- If the product is non-recipe, allow stock creation and stock editing through inventory endpoints.
- Update all API clients/constants from `/api/stock-management` to `/api/inventory`.

### Payment / Stock Note

After successful payment, stock deduction is now handled by a backend event listener in the inventory module. The frontend does not need to manually reduce stock after checkout.

### Deliverables

- Update all API clients and hooks.
- Refactor product create/edit pages and modal forms.
- Introduce separate inventory mutations for non-recipe products.
- Keep existing product list/detail views working with the returned `MenuResponse`.
- Ensure optimistic UI or refetch behavior keeps product and stock sections in sync after save.
