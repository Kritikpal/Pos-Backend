# Frontend Implementation Prompt: Tax Engine Refactor

You are working on the frontend for this POS system. The backend tax model has been refactored from a simple restaurant-level `TaxRate(name, amount)` setup into a generic tax engine with tax classes, tax definitions, tax rules, tax registrations, order tax context, and immutable order-time tax snapshots.

Your job is to update the frontend so it is fully compatible with the new backend contracts. Do not redesign unrelated parts of the app. Keep changes scoped to tax configuration, menu/catalog forms, cart/checkout/order flows, invoice/order display, and any offline/bootstrap sync client that consumes tax config.

## Primary Goal

Replace all frontend assumptions that taxes are just:

- `taxId`
- `taxName`
- `taxAmount`

with the new normalized model:

- `TaxClass`
- `TaxDefinition`
- `TaxRule`
- `TaxRegistration`
- order-level pricing totals
- per-tax summary
- per-line tax breakdown
- optional buyer tax context

## Important Constraints

- Do not keep using the old single-rate tax CRUD UI as the source of truth.
- Do not compute authoritative tax amounts in the frontend when the backend already returns them.
- Treat all money fields as decimal business values. Avoid ad hoc arithmetic for core totals if the backend already provides them.
- Preserve existing UI patterns and component conventions used by the frontend repo.
- If the frontend already has a POS cart, menu management screen, invoice screen, and admin settings area, extend those instead of creating disconnected new pages.

## Backend Reference Files

Use these backend files as the source of truth for request and response shapes:

- `src/main/java/com/kritik/POS/tax/route/TaxRoute.java`
- `src/main/java/com/kritik/POS/tax/controller/TaxController.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxClassRequest.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxClassResponseDto.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxDefinitionRequest.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxDefinitionResponseDto.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxRuleRequest.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxRuleResponseDto.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxRegistrationRequest.java`
- `src/main/java/com/kritik/POS/tax/dto/TaxRegistrationResponseDto.java`
- `src/main/java/com/kritik/POS/restaurant/models/request/ItemRequest.java`
- `src/main/java/com/kritik/POS/restaurant/models/request/MenuUpdateRequest.java`
- `src/main/java/com/kritik/POS/restaurant/dto/MenuItemResponseDto.java`
- `src/main/java/com/kritik/POS/restaurant/models/response/MenuResponse.java`
- `src/main/java/com/kritik/POS/order/model/request/InitiateOrderRequest.java`
- `src/main/java/com/kritik/POS/order/model/request/ConfiguredOrderInitiateRequest.java`
- `src/main/java/com/kritik/POS/order/model/request/OrderV2InitiateRequest.java`
- `src/main/java/com/kritik/POS/order/model/request/OrderTaxContextRequest.java`
- `src/main/java/com/kritik/POS/order/model/response/PaymentProcessingResponse.java`
- `src/main/java/com/kritik/POS/order/model/response/ConfiguredOrderResponse.java`
- `src/main/java/com/kritik/POS/order/model/response/OrderV2Response.java`
- `src/main/java/com/kritik/POS/order/model/response/OrderTaxSummaryResponse.java`
- `src/main/java/com/kritik/POS/order/model/response/OrderLineTaxResponse.java`
- `src/main/java/com/kritik/POS/order/model/response/OrderSaleItemResponse.java`
- `src/main/java/com/kritik/POS/order/model/response/ConfiguredSaleItemResponse.java`
- `src/main/java/com/kritik/POS/mobile/service/PosSyncServiceImpl.java`
- `src/main/java/com/kritik/POS/mobile/repository/PosSyncRepository.java`

Also note the backend wrapper structure:

- most APIs return `ApiResponse<T>` with `{ success, data, responseCode, message }`
- paginated endpoints return `PageResponse<T>` inside `data` with:
  - `items`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
  - `last`

## New Tax API Endpoints

Base:

- `GET /api/restaurants/tax/classes`
- `POST /api/restaurants/tax/classes`
- `DELETE /api/restaurants/tax/classes/{id}`
- `GET /api/restaurants/tax/definitions`
- `POST /api/restaurants/tax/definitions`
- `DELETE /api/restaurants/tax/definitions/{id}`
- `GET /api/restaurants/tax/rules`
- `POST /api/restaurants/tax/rules`
- `DELETE /api/restaurants/tax/rules/{id}`
- `GET /api/restaurants/tax/registrations`
- `POST /api/restaurants/tax/registrations`
- `DELETE /api/restaurants/tax/registrations/{id}`

## Enums the Frontend Must Support

- `TaxDefinitionKind`: `TAX`, `FEE`, `SERVICE_CHARGE`
- `TaxValueType`: `PERCENT`, `FIXED`
- `TaxCalculationMode`: `INCLUSIVE`, `EXCLUSIVE`
- `TaxCompoundMode`: `BASE_ONLY`, `ON_PREVIOUS_TAXES`

## What To Change

### 1. Replace legacy tax types and API clients

Find all frontend types, hooks, services, reducers, API modules, and forms that still assume a tax record looks like:

- `taxId`
- `taxName`
- `taxAmount`

Replace them with proper typed clients and models for:

- `TaxClass`
- `TaxDefinition`
- `TaxRule`
- `TaxRegistration`

Create clean TypeScript interfaces or frontend model types for:

- `TaxClassRequest`
- `TaxClassResponse`
- `TaxDefinitionRequest`
- `TaxDefinitionResponse`
- `TaxRuleRequest`
- `TaxRuleResponse`
- `TaxRegistrationRequest`
- `TaxRegistrationResponse`

### 2. Update tax settings/admin UI

Replace the old single tax screen with a tax configuration experience that supports at least these sections:

1. Tax Classes
2. Tax Definitions
3. Tax Rules
4. Tax Registrations

Minimum UI requirements:

- list table for each resource
- create/edit form
- delete action
- active status toggle or checkbox in form
- search and pagination where backend supports it
- restaurant filter support if the existing admin UI already supports chain/restaurant context

Suggested UX:

- Use tabs or segmented panels inside the existing tax/settings area.
- In rule forms, use dropdowns for `taxDefinitionId` and `taxClassId`.
- For enum fields, use selects with readable labels.
- Make it obvious whether a rule is inclusive or exclusive.
- Make it obvious whether a tax definition is a tax, fee, or service charge.
- Show effective dates and jurisdiction fields in rule lists.
- Show which registration is default.

### 3. Update menu item create/edit forms

The menu item form now needs tax-related catalog metadata.

Backend request fields added:

- `priceIncludesTax: Boolean`
- `taxClassId: Long`

Backend price fields are now decimal values instead of `Double`.

Update menu item create/edit UI to:

- load available tax classes for the selected restaurant
- allow choosing a `taxClassId`
- allow toggling `priceIncludesTax`
- send `itemPrice` and `disCount` in the updated request shape
- render existing values when editing

Also update any menu list/details UI to display:

- `taxClassId` or resolved tax class name if you can hydrate it
- `priceIncludesTax`

Important:

- if the frontend previously had any direct per-item tax selector tied to individual taxes, remove or deprecate that path
- the product taxability model is now tax class based, not direct menu-item-to-tax mapping

### 4. Update cart and checkout request payloads

Order creation/update requests now optionally accept:

- `taxContext`

`taxContext` shape:

- `buyerName`
- `buyerTaxId`
- `buyerTaxCategory`
- `buyerCountryCode`
- `buyerRegionCode`
- `billingAddressJson`
- `placeOfSupplyCountryCode`
- `placeOfSupplyRegionCode`

Update the checkout UI so it can capture this information when needed.

Minimum UX:

- make buyer tax details optional
- hide advanced fields behind an "Add tax/invoice details" expandable section if that fits the existing UX
- persist the values during cart editing and order update flow
- send them in `InitiateOrderRequest`, `ConfiguredOrderInitiateRequest`, `OrderV2InitiateRequest`, and their update variants where relevant

If the app has multiple order flows:

- update the one actually used in production first
- if both legacy and v2 flows are present in the frontend, prefer wiring the modern order v2 flow

### 5. Update cart, payment, and order detail display

The frontend must stop assuming the backend returns only `totalPrice`.

New response totals include:

- `totalPrice`
- `subtotalAmount`
- `discountAmount`
- `taxableAmount`
- `taxAmount`
- `feeAmount`
- `roundingAmount`
- `grandTotal`

Update cart summary, payment summary, and order detail views to render the new breakdown cleanly.

Minimum display requirements:

- subtotal
- discount
- taxable amount
- tax amount
- fee amount if non-zero
- rounding amount if non-zero
- grand total

Do not recalculate these in the frontend if the backend already returns them.

### 6. Render per-tax and per-line tax breakdowns

New order responses can include:

- `taxSummaries: OrderTaxSummaryResponse[]`
- `lineTaxes: OrderLineTaxResponse[]`

Update order detail / receipt / invoice preview UI to render:

- order-level tax summaries by tax code/name
- line-level tax rows when available

Useful fields from `OrderTaxSummaryResponse`:

- `taxDefinitionCode`
- `taxDisplayName`
- `taxableBaseAmount`
- `taxAmount`
- `currencyCode`

Useful fields from `OrderLineTaxResponse`:

- `referenceKey`
- `taxDefinitionCode`
- `taxDisplayName`
- `valueType`
- `rateOrAmount`
- `calculationMode`
- `compoundMode`
- `sequenceNo`
- `taxableBaseAmount`
- `taxAmount`
- `currencyCode`
- `jurisdictionCountryCode`
- `jurisdictionRegionCode`

Only show line-level detail where it improves clarity. Avoid cluttering small mobile checkout surfaces.

### 7. Update order line rendering

`OrderSaleItemResponse` and `ConfiguredSaleItemResponse` now include tax snapshot information.

For regular sale items:

- `taxClassCodeSnapshot`
- `priceIncludesTax`
- `lineTaxAmount`

For configured sale items:

- `taxClassCodeSnapshot`
- `priceIncludesTax`
- `lineTaxAmount`
- selection entry `priceDelta` is decimal

Update UI types and components so these fields are not dropped.

### 8. Update invoice-facing UI

The backend now supports seller and buyer tax context for invoice display.

If the frontend has invoice screens, printable invoice views, or order receipt detail modals, update them to show:

- seller registration number
- seller legal name if available
- buyer name
- buyer tax ID
- buyer tax category
- place of supply where useful
- subtotal / tax / fee / grand total
- tax summary

### 9. Update mobile/bootstrap sync client if it exists in the frontend repo

If the frontend repo contains POS bootstrap/pull sync logic, update it for the new tax engine.

The sync payload now includes these groups in addition to the compatibility `taxes` group:

- `taxClasses`
- `taxDefinitions`
- `taxRules`
- `taxRegistrations`

Also update catalog sync types:

- `MenuItemSyncDto` now includes `taxClassId`
- `MenuPriceSyncDto` now includes decimal `price`, decimal `discount`, and `priceIncludesTax`

Do not keep relying only on the legacy `taxes` sync stream.

## Data Contract Summary

### Tax Class

- `id`
- `restaurantId`
- `code`
- `name`
- `description`
- `isExempt`
- `isActive`
- `createdAt`
- `updatedAt`

### Tax Definition

- `id`
- `restaurantId`
- `code`
- `displayName`
- `kind`
- `valueType`
- `defaultValue`
- `currencyCode`
- `isRecoverable`
- `isActive`
- `createdAt`
- `updatedAt`

### Tax Rule

- `id`
- `restaurantId`
- `taxDefinitionId`
- `taxClassId`
- `calculationMode`
- `compoundMode`
- `sequenceNo`
- `validFrom`
- `validTo`
- `countryCode`
- `regionCode`
- `buyerTaxCategory`
- `minAmount`
- `maxAmount`
- `priority`
- `isActive`
- `createdAt`
- `updatedAt`

### Tax Registration

- `id`
- `restaurantId`
- `schemeCode`
- `registrationNumber`
- `legalName`
- `countryCode`
- `regionCode`
- `placeOfBusiness`
- `isDefault`
- `validFrom`
- `validTo`
- `isActive`
- `createdAt`
- `updatedAt`

## Recommended Implementation Order

1. Inspect the frontend repo structure and identify:
   - API client layer
   - tax settings pages
   - menu item forms
   - cart/checkout flow
   - order details/invoice UI
   - sync/offline client if present
2. Replace old tax types and API service modules.
3. Update tax settings UI to the new four-resource model.
4. Update menu item create/edit forms with `taxClassId` and `priceIncludesTax`.
5. Update checkout request builders with `taxContext`.
6. Update cart/order responses to use the new totals and tax summaries.
7. Update invoice/order details UI.
8. Update sync client if the repo has one.
9. Run the frontend test/build/lint commands and fix type errors.

## Acceptance Criteria

The work is complete only when all of the following are true:

- no frontend code still depends on legacy single-tax CRUD as the main flow
- tax settings UI supports classes, definitions, rules, and registrations
- menu item forms support `taxClassId` and `priceIncludesTax`
- checkout can send optional buyer tax context
- order/cart/payment views show the new amount breakdown
- order detail or invoice UI shows tax summaries and line taxes when returned
- any sync client supports the new tax-engine groups
- TypeScript types and API clients match backend contracts
- frontend build passes

## Output Expectations

When you finish:

- summarize which screens and modules changed
- list any backend assumptions you relied on
- call out any place where the frontend still has a temporary compatibility path
- include any follow-up gaps that require product clarification

