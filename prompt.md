# Frontend Implementation Prompt: Inventory Unit Conversion Refactor

You are working on the frontend for this POS system. The backend inventory model has been refactored to support:

- global unit definitions via `UnitMaster`
- item-specific, restaurant-scoped unit conversions
- base-unit storage only for stock quantities
- unit-aware stock receipt entry
- compatibility fields so old `unitOfMeasure` reads still work during migration

Your job is to update the frontend so it is fully compatible with the new backend contracts. Do not redesign unrelated parts of the app. Keep changes scoped to inventory, ingredient management, direct-stock item management, stock receipt flow, menu/direct stock metadata where applicable, and any sync/bootstrap client that consumes inventory data.

## Primary Goal

Replace all frontend assumptions that an inventory item only has a single free-text unit string with the new normalized model:

- `UnitMaster`
- `baseUnit`
- `ItemUnitConversion`
- `enteredQty + unitId` for stock receipts
- stock quantities always represented in base units in backend persistence

The frontend must allow users to define and use alternate units such as:

- Egg: `1 CARTON = 30 PCS`
- Chocolate: `1 CARTON = 100 PCS`

without introducing mixed-unit stock storage in the UI state or backend payload expectations.

## Important Constraints

- Do not add carton/box/dozen-specific UI fields or hardcoded columns.
- Do not assume conversions are global; they are item-specific and restaurant-specific.
- Do not compute or persist separate stock buckets per unit.
- Treat `unitOfMeasure` as a temporary compatibility field for display only when needed.
- Use `baseUnit` and `conversions` as the source of truth for editing workflows.
- Preserve existing UI patterns and component conventions used by the frontend repo.
- If the frontend already has ingredient forms, direct item stock forms, receipt entry screens, and inventory list/detail views, extend those instead of creating disconnected pages.

## Backend Contract Mode

The frontend is a separate project. Do not depend on backend source files, imports, or shared code from the backend repository.

Treat the backend as an external API-only system. Use:

- existing frontend API client conventions
- observed API payloads from Swagger/OpenAPI, Postman, network responses, or backend-provided contract docs
- the contract summary in this prompt as the source of truth unless real API responses prove otherwise

Also note the backend wrapper structure:

- most APIs return `ApiResponse<T>` with `{ success, data, responseCode, message }`
- paginated endpoints return `PageResponse<T>` inside `data` with:
  - `items`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
  - `last`

## API Discovery Rules

- Do not assume direct access to backend internals.
- If the frontend repo already has API clients for inventory, extend those first.
- If exact endpoint paths for inventory CRUD are not already documented in the frontend repo, infer them from existing client modules or Swagger.
- If a field is missing from current API clients but is present in real API responses, update the clients and types to match the API.
- Do not generate frontend code that requires importing backend enums or Java DTOs.

## Expected API Contracts

The frontend should work against API payloads shaped like the following.

### UnitSummary

```json
{
  "id": 1,
  "code": "PCS",
  "displayName": "PCS",
  "active": true
}
```

### ItemUnitConversion

```json
{
  "id": 11,
  "unit": {
    "id": 2,
    "code": "CARTON",
    "displayName": "CARTON",
    "active": true
  },
  "factorToBase": 30,
  "purchaseAllowed": true,
  "saleAllowed": false,
  "active": true
}
```

### Ingredient read response

```json
{
  "sku": "ING-1",
  "restaurantId": 10,
  "ingredientName": "Egg",
  "description": "Farm eggs",
  "category": "Dairy",
  "totalStock": 240,
  "reorderLevel": 60,
  "unitOfMeasure": "PCS",
  "baseUnit": {
    "id": 1,
    "code": "PCS",
    "displayName": "PCS",
    "active": true
  },
  "conversions": [
    {
      "id": 1,
      "unit": { "id": 1, "code": "PCS", "displayName": "PCS", "active": true },
      "factorToBase": 1,
      "purchaseAllowed": true,
      "saleAllowed": true,
      "active": true
    },
    {
      "id": 2,
      "unit": { "id": 2, "code": "CARTON", "displayName": "CARTON", "active": true },
      "factorToBase": 30,
      "purchaseAllowed": true,
      "saleAllowed": false,
      "active": true
    }
  ],
  "isActive": true
}
```

### Direct stock item read response

```json
{
  "sku": "SKU-1",
  "restaurantId": 10,
  "menuItemId": 15,
  "itemName": "Chocolate Box",
  "totalStock": 100,
  "reorderLevel": 20,
  "unitOfMeasure": "PCS",
  "baseUnit": {
    "id": 1,
    "code": "PCS",
    "displayName": "PCS",
    "active": true
  },
  "conversions": [
    {
      "id": 3,
      "unit": { "id": 1, "code": "PCS", "displayName": "PCS", "active": true },
      "factorToBase": 1,
      "purchaseAllowed": true,
      "saleAllowed": true,
      "active": true
    },
    {
      "id": 4,
      "unit": { "id": 3, "code": "BOX", "displayName": "BOX", "active": true },
      "factorToBase": 10,
      "purchaseAllowed": true,
      "saleAllowed": true,
      "active": true
    }
  ],
  "isActive": true,
  "isAvailable": true
}
```

### Ingredient/direct item write payload shape

The frontend should be able to send request payloads shaped like:

```json
{
  "baseUnitId": 1,
  "unitOfMeasure": "PCS",
  "conversions": [
    {
      "unitId": 1,
      "factorToBase": 1,
      "purchaseAllowed": true,
      "saleAllowed": true,
      "active": true
    },
    {
      "unitId": 2,
      "factorToBase": 30,
      "purchaseAllowed": true,
      "saleAllowed": false,
      "active": true
    }
  ]
}
```

This is in addition to the existing ingredient or direct-stock metadata fields already used by the frontend.

### Receipt SKU option response

```json
{
  "sku": "ING-1",
  "skuName": "Egg",
  "skuType": "INGREDIENT",
  "unit": "PCS",
  "availableStock": 240,
  "baseUnit": {
    "id": 1,
    "code": "PCS",
    "displayName": "PCS",
    "active": true
  },
  "purchaseUnits": [
    {
      "id": 1,
      "unit": { "id": 1, "code": "PCS", "displayName": "PCS", "active": true },
      "factorToBase": 1,
      "purchaseAllowed": true,
      "saleAllowed": true,
      "active": true
    },
    {
      "id": 2,
      "unit": { "id": 2, "code": "CARTON", "displayName": "CARTON", "active": true },
      "factorToBase": 30,
      "purchaseAllowed": true,
      "saleAllowed": false,
      "active": true
    }
  ]
}
```

### Receipt create payload

```json
{
  "restaurantId": 10,
  "supplierId": 8,
  "invoiceNumber": "INV-1001",
  "receivedAt": "2026-04-22T10:00:00",
  "notes": "Weekly restock",
  "items": [
    {
      "sku": "ING-1",
      "skuType": "INGREDIENT",
      "enteredQty": 2,
      "unitId": 2,
      "unitCost": 150
    }
  ]
}
```

Compatibility note:

- `quantityReceived` may still exist in some payloads or clients, but new frontend work should use `enteredQty` as the primary field.

## New/Updated Inventory Data Model

### UnitSummaryResponse

- `id`
- `code`
- `displayName`
- `active`

### ItemUnitConversionResponse

- `id`
- `unit: UnitSummaryResponse`
- `factorToBase`
- `purchaseAllowed`
- `saleAllowed`
- `active`

### IngredientResponse

Existing fields still include:

- `sku`
- `restaurantId`
- `ingredientName`
- `description`
- `category`
- `totalStock`
- `reorderLevel`
- `unitOfMeasure`
- `isActive`
- timestamps

New fields added:

- `baseUnit: UnitSummaryResponse`
- `conversions: ItemUnitConversionResponse[]`

### StockResponse / direct stock item response

Existing fields still include:

- `sku`
- `restaurantId`
- `menuItemId`
- `itemName`
- `totalStock`
- `reorderLevel`
- `unitOfMeasure`
- `isActive`
- `isAvailable`

New fields added:

- `baseUnit: UnitSummaryResponse`
- `conversions: ItemUnitConversionResponse[]`

### StockReceiptSkuOptionDto

- `sku`
- `skuName`
- `skuType`
- `unit`
- `availableStock`
- `baseUnit: UnitSummaryResponse`
- `purchaseUnits: ItemUnitConversionResponse[]`

### StockReceiptCreateRequest.ReceiptItemRequest

Frontend must support:

- `sku`
- `skuType`
- `enteredQty`
- `quantityReceived`
- `unitId`
- `unitCost`

Compatibility note:

- `enteredQty` is the user-entered quantity in the selected unit.
- `quantityReceived` is still present for compatibility but should not be treated as the primary field for new UI.
- `unitId` identifies the selected purchase unit. If omitted, backend treats the base unit as default.

### Mobile sync additions

Both `IngredientStockSyncDto` and `ItemStockSyncDto` now include:

- `unitOfMeasure`
- `baseUnitId`
- `baseUnitCode`

`unitOfMeasure` remains available for compatibility, but frontend sync/state models should prefer base-unit fields.

## Enums the Frontend Must Support

### UnitConversionSourceType

- `INGREDIENT`
- `DIRECT_ITEM`

### StockReceiptSkuType

Backend still returns:

- `INGREDIENT`
- `DIRECT_MENU`

Important:

- keep `DIRECT_MENU` support in frontend receipt flows for compatibility
- map it conceptually to the direct-item conversion workflow

## What To Change

### 1. Replace legacy unit string assumptions in types and API clients

Find all frontend types, hooks, services, reducers, form schemas, and API modules that assume inventory units are only:

- `unitOfMeasure: string`

Update them to support:

- `baseUnit`
- `conversions`
- `unitId`
- `baseUnitId`
- `baseUnitCode`

Create or update TypeScript interfaces for:

- `UnitSummary`
- `ItemUnitConversion`
- `UnitConversionRequest`
- `IngredientRequest`
- `IngredientResponse`
- `ItemStockUpsertRequest`
- `StockUpdateRequest`
- `StockResponse`
- `StockReceiptSkuOption`
- `StockReceiptCreateRequest`
- `StockReceiptItemRequest`
- sync DTOs for ingredient and direct item stock

### 2. Update ingredient create/edit UI

The ingredient form now needs unit configuration, not just a raw unit string.

Update ingredient create/edit UI to:

- load available global units if the frontend has a unit picker source
- allow selecting a `baseUnitId`
- allow editing alternate unit conversions as a repeatable list
- allow configuring for each conversion:
  - unit
  - factor to base
  - purchaseAllowed
  - saleAllowed
  - active
- always ensure the base unit is represented in UI as factor `1`

Compatibility behavior:

- if existing screens still bind to `unitOfMeasure`, initialize it from `baseUnit.code`
- when editing existing ingredients, hydrate the form from `baseUnit` + `conversions`

### 3. Update direct stock item metadata UI

The direct stock save/update flow now supports base unit and conversions for direct items backed by `MenuItem`.

Update direct stock item forms or drawers to:

- allow selecting `baseUnitId`
- allow editing `conversions[]`
- keep reorder level and active state behavior unchanged
- continue displaying `unitOfMeasure`, but derive it from `baseUnit.code`

Important:

- do not treat direct stock items as having separate quantity fields per unit
- all displayed stock totals still represent base-unit quantity

### 4. Update stock receipt entry flow

This is the most important frontend change.

The stock receipt screen must now:

- show available purchase units for the selected SKU from `purchaseUnits`
- let the user enter:
  - item/SKU
  - unit
  - entered quantity
  - unit cost
- send `enteredQty` and `unitId`
- stop assuming the entry unit is always the base unit

Receipt UX requirements:

- when a SKU is selected, default the unit picker to its base unit
- if alternate purchase units exist, show them in the unit dropdown with readable labels like:
  - `CARTON (30 PCS)`
  - `PCS (1 PCS)`
- show available stock in base unit terms
- make it clear that stock will be added in base units after conversion
- allow old payload fallback only if the existing screen cannot provide a unit picker yet

### 5. Update inventory list/detail screens

Ingredient and direct-stock detail views should now show:

- current stock in base units
- reorder level in base units
- base unit
- available alternate conversions

Recommended display:

- Base unit: `PCS`
- Conversions:
  - `1 PCS = 1 PCS`
  - `1 CARTON = 30 PCS`

Do not present conversions as separate stock balances.

### 6. Update menu and direct item read models where unit data is shown

Menu and stock summary DTOs now include base-unit metadata in addition to `unitOfMeasure`.

Update any menu/direct stock list cards, detail pages, or admin tables that show unit information to:

- continue showing `unitOfMeasure` for compatibility
- prefer `baseUnitCode` or `baseUnit.code` where available
- avoid duplicating or contradicting unit labels

### 7. Update validation and form behavior

Frontend validation must enforce:

- base unit selection is required
- conversion factor must be greater than `0`
- duplicate units are not allowed within one item's conversion list
- base unit row must effectively resolve to factor `1`
- for direct items, warn or block values that would imply fractional base stock if your UI can detect it

Recommended behavior:

- treat the base-unit row as fixed and non-removable
- let users toggle purchase/sale flags per unit

### 8. Update sync/offline/bootstrap client if present

If the frontend repo contains bootstrap/pull sync logic, update it so inventory state includes:

- `baseUnitId`
- `baseUnitCode`
- compatibility `unitOfMeasure`

Do not assume the synced unit string is the only canonical unit anymore.

If there is offline receipt drafting:

- store selected `unitId`
- store entered quantity separately from converted quantity
- only rely on backend for authoritative conversion when receipt is submitted, unless the app already has a shared conversion utility

### 9. Keep compatibility paths explicit

The backend intentionally keeps compatibility fields during migration.

Frontend should:

- still read `unitOfMeasure`
- prefer `baseUnit` / `baseUnitCode` for new logic
- only fall back to `unitOfMeasure` when the new fields are absent

Avoid building fresh UI on the compatibility field alone.

## Suggested UI/UX Changes

### Ingredient and direct item forms

- Add a `Base Unit` select
- Add a `Conversions` editable grid/table
- Columns:
  - Unit
  - Factor to Base
  - Purchase Allowed
  - Sale Allowed
  - Active
- Make the base unit row pinned at the top

### Receipt item rows

- Replace old `Quantity Received` field with:
  - `Unit`
  - `Entered Quantity`
- Keep cost entry as-is
- If useful, show helper text:
  - `Will add 60 PCS to stock`

### Inventory details

- Show stock as:
  - `Available Stock: 240 PCS`
- Show conversions separately:
  - `Purchase Units`
  - `Sale Units`

## Recommended Implementation Order

1. Inspect the frontend repo structure and identify:
   - API client layer
   - inventory/ingredient forms
   - direct stock item forms
   - stock receipt screen
   - inventory detail/list screens
   - sync/offline client if present
2. Update TypeScript models and API service modules for new inventory/unit contracts.
3. Update ingredient create/edit flow with `baseUnitId` and `conversions`.
4. Update direct stock item metadata flow with `baseUnitId` and `conversions`.
5. Update stock receipt SKU option handling and receipt item entry UI.
6. Update inventory read-model screens to display base unit and conversion summaries.
7. Update sync/offline models to carry base-unit metadata.
8. Run frontend test/build/lint commands and fix type/runtime issues.

## Acceptance Criteria

The work is complete only when all of the following are true:

- ingredient forms support `baseUnitId` and `conversions`
- direct stock item forms support `baseUnitId` and `conversions`
- receipt entry sends `enteredQty` and `unitId`
- receipt UI can show alternate purchase units for a SKU
- inventory/detail screens show base unit and conversion definitions clearly
- no new UI stores separate stock quantities for carton/box/dozen
- frontend models support `baseUnit`, `baseUnitCode`, and `conversions`
- compatibility with legacy `unitOfMeasure` reads is preserved
- frontend build passes

## Output Expectations

When you finish:

- summarize which frontend screens and modules changed
- list the backend contract assumptions you relied on
- call out any remaining compatibility fallback that still depends on `unitOfMeasure`
- note any UX gaps that need product clarification, especially around:
  - who can define sale vs purchase units
  - whether direct items should allow fractional display quantities
  - whether frontend should preview converted base quantity before receipt submission
