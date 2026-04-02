# Owner Panel Design System – Professional & Data-Driven

## Overview
The Owner Panel is a business-focused management interface designed for mess owners to efficiently manage operations, approve requests, handle orders, track payments, and view analytics. The design prioritizes clarity, trust, and quick decision-making through a professional palette and task-oriented layout.

---

## Design Philosophy

### Goals
- **Efficient Dashboard** – At-a-glance business metrics (enrolled users, pending orders, today's earnings)
- **Task-Oriented Navigation** – Clear tabs and sections for orders, requests, users, and payments
- **Data Visualization** – Clean, professional charts for revenue trends and order analytics
- **Authority & Trustworthiness** – Royal blue and steel gray convey reliability and professionalism
- **Action-Focused UI** – Golden amber highlights for important actions (approve, mark paid)

---

## Color Palette

| Color Name | Hex Code | Usage | Semantic |
|------------|----------|-------|----------|
| **Owner Primary** | `#2C3E66` | Main text, headings, primary bars | Reliability, authority |
| **Owner Secondary** | `#6C7A89` | Body text, borders, subtle backgrounds | Supporting text, neutral |
| **Owner Accent** | `#F39C12` | Action buttons, highlights, notifications | Call-to-action, approval |
| **Owner Background** | `#ECF0F1` | Screen background | Clean, non-fatiguing |
| **Owner Card Background** | `#FFFFFF` | Card surfaces, content areas | Clear content hierarchy |
| **Owner Card Border** | `#D5DDE5` | Card outlines, dividers | Subtle structure |
| **Owner Success** | `#2E7D32` | Positive states, accepted orders | Confirmation, success |
| **Owner Danger** | `#B23A48` | Error states, rejected items | Attention, danger |
| **Owner Info** | `#2C7FB8` | Informational states | Secondary actions |

**How to Use:**
```xml
<!-- In layouts -->
android:textColor="@color/owner_primary"
android:background="@color/owner_bg"
app:strokeColor="@color/owner_card_border"

<!-- In styles -->
<item name="colorPrimary">@color/owner_primary</item>
```

---

## Typography

### Text Styles (Android Typeface Mapping)

| Style | Font | Size | Weight | Color | Usage |
|-------|------|------|--------|-------|-------|
| **Heading** | sans-serif | 22sp | bold | owner_primary | Screen titles (Dashboard, Payments, Analytics) |
| **Section Title** | sans-serif | 18sp | bold | owner_primary | Major sections (Requests Queue, Metrics) |
| **Body** | sans-serif | 15sp | regular | owner_secondary | Descriptive text, list labels |
| **Data** | sans-serif-medium | 20sp | bold | owner_primary | Metric values, numbers (orders count) |
| **Button Text** | sans-serif-medium | 14sp | regular | white / secondary | Call-to-action labels |

**Text Appearance Styles:**
- `TextAppearance.MyMess.Owner.Heading` – Page titles
- `TextAppearance.MyMess.Owner.SectionTitle` – Section headings
- `TextAppearance.MyMess.Owner.Body` – Regular content text
- `TextAppearance.MyMess.Owner.Data` – Large metric values

**Example:**
```xml
<TextView
    android:textAppearance="@style/TextAppearance.MyMess.Owner.Heading"
    android:text="Owner Dashboard" />

<TextView
    android:textAppearance="@style/TextAppearance.MyMess.Owner.Data"
    android:text="148" />
```

---

## Component Styles

### Cards
- **Style Name:** `Widget.MyMess.Owner.Card`
- **Background:** White (`#FFFFFF`)
- **Border:** 1dp steel gray (`#D5DDE5`)
- **Corner Radius:** 8dp
- **Padding:** 12dp internal, 10dp bottom margin
- **Purpose:** Contain list items, metric cards, and content sections

```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.MyMess.Owner.Card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="10dp">
    <!-- Content -->
</com.google.android.material.card.MaterialCardView>
```

### Buttons

#### Primary Action (Accent) Button
- **Style Name:** `Widget.MyMess.Owner.ButtonAccent`
- **Background:** Golden amber (`#F39C12`)
- **Text Color:** White
- **Purpose:** Approve, accept, mark paid, advance order
- **Prominence:** Highest; use for primary actions

```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MyMess.Owner.ButtonAccent"
    android:text="Accept" />
```

#### Secondary (Outline) Button
- **Style Name:** `Widget.MyMess.Owner.ButtonOutline`
- **Border:** Steel gray
- **Text Color:** Steel gray
- **Background:** Transparent
- **Purpose:** Reject, cancel, delete, secondary options
- **Prominence:** Lower; use for optional or destructive actions

```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MyMess.Owner.ButtonOutline"
    android:text="Reject" />
```

### Text Input Fields
- **Box Background:** White (`#FFFFFF`)
- **Box Stroke Color:** Steel gray (`#6C7A89`)
- **Hint Text:** Secondary gray
- **Purpose:** Search, filter fields

```xml
<com.google.android.material.textfield.TextInputLayout
    android:hint="Search by name"
    app:boxBackgroundColor="@color/owner_card_bg"
    app:boxStrokeColor="@color/owner_secondary">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etSearch"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>
```

---

## Layouts & Screens

### 1. Owner Dashboard (`fragment_owner_home.xml`)
**Purpose:** Quick overview of business metrics and active requests

**Key Elements:**
- **Header:** "Owner Dashboard" (Heading style) + subtitle
- **Banner Carousel:** Promotional/informational banners
- **Metric Cards** (3-column grid):
  - Enrolled Users (count)
  - Pending Orders (count)
  - Today's Earnings (currency)
  - Each in a separate card with Owner Card style
- **Requests Queue Section:**
  - TabLayout with two tabs: "Order Requests" | "Join Requests"
  - ViewPager2 with corresponding fragments
- **Bottom Navigation:** Navigation to Orders, Meals, Profile

**Color Scheme:**
- Background: `owner_bg`
- Cards: `owner_card_bg` with `owner_card_border`
- Text: `owner_primary` for headings, `owner_secondary` for labels
- Accent: `owner_accent` for tab indicator

---

### 2. Pending Orders (`fragment_owner_pending_orders.xml`)
**Purpose:** View and manage orders waiting for approval or in progress

**Key Elements:**
- **Header:** "Pending Orders" + description
- **List:** RecyclerView of order cards (item_owner_order.xml)
  - Each card: Meal name (SectionTitle), meta (Body), status (Body+info color)
  - Action button: "Advance" (ButtonAccent)
- **Bottom Navigation:** Access to other owner sections

**Color Scheme:**
- Cards: Owner Card style
- Status text: `owner_info` for in-progress state
- Buttons: Accent buttons for advancing orders

---

### 3. Join Requests (`fragment_owner_requests.xml`)
**Purpose:** Approve or reject user enrollment requests

**Key Elements:**
- **Header:** "Join Requests" + description
- **Search/Filter:** (Optional) Quick filter by user name
- **Refresh Button:** Outlined style button to reload list
- **List:** RecyclerView of request cards (item_owner_request.xml)
  - Each card: User name (SectionTitle), meta (Body)
  - Action buttons: "Accept" (ButtonAccent) | "Reject" (ButtonOutline)
- **Empty State:** "No join requests pending" (centered, secondary text)

**Color Scheme:**
- Buttons: Accent (approve) vs. Outline (reject)
- Card styling: Owner Card standard

---

### 4. Enrolled Users (`fragment_owner_users.xml`)
**Purpose:** Browse and manage enrolled users

**Key Elements:**
- **Header:** "Enrolled Users"
- **Search Field:** TextInputLayout with "Search by name or email"
- **List:** RecyclerView of user cards (item_owner_user.xml)
  - Each card: User name (SectionTitle), email (Body), status (Body+info color)
- **Click Interaction:** (Planned) Open user details to block/manage

**Color Scheme:**
- Search box: White background with gray border
- User status: `owner_info` color for active status

---

### 5. Payments (`fragment_owner_payments.xml`)
**Purpose:** Track and manage user payments and monthly bills

**Key Elements:**
- **Header:** "Owner Payments"
- **Search Field:** "Search by user name or status"
- **Status Filter:** AutoCompleteTextView dropdown (Pending / Paid)
- **Bill State Filter:** AutoCompleteTextView dropdown
- **Refresh Button:** Outlined style
- **List:** RecyclerView of payment records (item_owner_payment.xml – to be created)
  - Each item: User name, amount, due date, payment status, action button
- **Click Action:** "Mark Paid" (ButtonAccent) for pending payments

**Color Scheme:**
- Filter boxes: TextInputLayout with white background and gray stroke
- Payment status: Color-coded text (pending=secondary, paid=success)

---

### 6. Analytics (`fragment_owner_analytics.xml`)
**Purpose:** Visualize business trends and performance metrics

**Key Elements:**
- **Header:** "Owner Analytics" + summary text
- **Refresh Button:** Outlined style
- **Charts (in MaterialCardView containers):**
  - **Orders per Day:** BarChart with owner_primary bars
  - **Revenue Trend:** LineChart with owner_primary line
  - **Top Meals:** BarChart with owner_accent bars
  - **User Growth:** LineChart with owner_info line
- **Chart Styling:** Use `OwnerChartStyler` utility for consistent appearance
  - Legend text: Secondary gray
  - Axis labels: Secondary gray
  - Grid lines: Light card border color

**Color Scheme:**
- Charts wrapped in Owner Card style containers
- Chart colors: Primary (revenue), Accent (highlights), Success (positive), Info (user growth)
- Use OwnerChartStyler.kt for consistent theming

---

### 7. Meals Management (`fragment_owner_meals.xml`)
**Purpose:** Manage mess meals and cloud meals

**Key Elements:**
- **Header:** "Meals Management"
- **TabLayout:** Two tabs: "Mess Meals" | "Cloud Meals"
- **ViewPager2:** Fragment per meal type
- **Add Button:** (In fragment) FAB or top button to add new meal
- **List:** RecyclerView of meals (item_owner_cloud_meal.xml)
  - Each card: Meal name (SectionTitle), meta (Body), action buttons
  - Buttons: "Edit" (Outline) | "Disable" (Outline) | "Delete" (Outline)

**Color Scheme:**
- Tabs: Accent indicator, primary text
- Outline buttons for secondary actions
- Cards: Standard Owner Card style

---

## Reusable Components

### 1. Metric Card Include (`include_owner_metric_card.xml`)
Reusable card for displaying a single metric (count, currency, etc.)

```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.MyMess.Owner.Card"
    android:layout_width="0dp"
    android:layout_height="wrap_content">
    <LinearLayout>
        <TextView android:id="@+id/tvMetricLabel" style="Body" />
        <TextView android:id="@+id/tvMetricValue" style="Data" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

**Usage in Dashboard:**
```xml
<LinearLayout android:orientation="horizontal" android:layout_weight="1">
    <include layout="@layout/include_owner_metric_card" />
    <!-- Repeat for 3 metrics with layout_weight="1" each -->
</LinearLayout>
```

### 2. Section Header Include (`include_owner_section_header.xml`)
Reusable header with title and divider for logical grouping

```xml
<LinearLayout>
    <TextView android:id="@+id/tvSectionTitle" style="SectionTitle" />
    <View android:background="@color/owner_card_border" />
</LinearLayout>
```

### 3. Empty State View (`view_owner_empty_state.xml`)
Reusable centered empty state with title and description

```xml
<LinearLayout android:gravity="center">
    <TextView android:id="@+id/tvEmptyTitle" style="SectionTitle" />
    <TextView android:id="@+id/tvEmptyDescription" style="Body" />
</LinearLayout>
```

---

## Chart Styling Utility (`OwnerChartStyler.kt`)

A Kotlin utility to apply consistent chart styling across analytics screens.

**Key Methods:**
- `styleLineDataSet(dataSet, label)` – Blue line for primary trends
- `styleLineDataSetSuccess(dataSet, label)` – Green line for positive growth
- `styleLineDataSetInfo(dataSet, label)` – Info blue for user metrics
- `styleBarDataSet(dataSet, label)` – Blue bars for standard metrics
- `styleBarDataSetAccent(dataSet, label)` – Amber bars for highlights
- `styleBaseChart(chart)` – Apply axis, legend, and text styling

**Example Usage:**
```kotlin
val revenueSet = LineDataSet(entries, "Revenue")
OwnerChartStyler.styleLineDataSet(revenueSet)
binding.chartRevenue.data = LineData(revenueSet)
OwnerChartStyler.styleBaseChart(binding.chartRevenue)
binding.chartRevenue.invalidate()
```

---

## Navigation Bottom Menu (`owner_bottom_nav_menu.xml`)

Four items for main owner flows:
- **Home** – Dashboard (icon_home)
- **Orders** – Pending orders (icon_orders)
- **Meals** – Meal management (icon_meals)
- **Profile** – Owner profile/settings (icon_profile)

**Styling:**
- Item icons: `owner_primary`
- Item text: `owner_primary`
- Active indicator: `owner_accent`
- Background: `owner_card_bg`

```xml
<item android:id="@+id/nav_owner_home"
    android:icon="@drawable/ic_home"
    android:title="@string/home" />
<!-- Repeat for Orders, Meals, Profile -->
```

---

## Spacing & Dimensions

| Element | Dimension |
|---------|-----------|
| Screen Padding | 16dp |
| Card Margin Bottom | 10dp |
| Card Corner Radius | 8dp |
| Card Padding | 12dp |
| Metric Card Height | wrap_content |
| Section Margin Top | 16dp |
| Button Margin Top | 8dp |
| Text Margin Top (within card) | 4–6dp |
| List Item Padding Bottom | 8dp |

---

## Animation & Transitions

- **Card Elevation:** 2dp (subtle shadow for depth)
- **Button Ripple:** Material Design ripple effect (automatic with MaterialButton)
- **Fragment Transitions:** Fade or slide (default Navigation Component)
- **Loading States:** ProgressBar centered, secondary gray color

---

## Accessibility

- **Contrast:** WCAG AA compliant (primary text on white: 7.5:1 ratio)
- **Text Size:** Minimum 14sp for body, 18sp for data
- **Touch Targets:** Buttons minimum 48dp (Material Design standard)
- **Content Descriptions:** Add android:contentDescription for icon buttons
- **Color Not Sole Indicator:** Status conveyed via text + color (e.g., "Pending (gray)" not just gray)

---

## Implementation Checklist

- [x] Color palette defined in `colors.xml`
- [x] Typography styles in `owner_styles.xml`
- [x] Component button styles in `owner_styles.xml`
- [x] Dashboard layout with metric cards (`fragment_owner_home.xml`)
- [x] Pending orders screen (`fragment_owner_pending_orders.xml`)
- [x] Join requests screen (`fragment_owner_requests.xml`)
- [x] Enrolled users screen with search (`fragment_owner_users.xml`)
- [x] Payments screen with filters (`fragment_owner_payments.xml`)
- [x] Analytics screen with charts (`fragment_owner_analytics.xml`)
- [x] Meals management screen (`fragment_owner_meals.xml`)
- [x] Item layouts with consistent styling (`item_owner_*.xml`)
- [x] Reusable component includes (`include_owner_*.xml`)
- [x] Chart styling utility (`OwnerChartStyler.kt`)
- [x] String resources for all UI text (`strings.xml`)
- [ ] Fragment/ViewModel implementations with chart integration
- [ ] Fragment/ViewModel implementations with state management
- [ ] FAB or add meal button implementation
- [ ] Payment detail screen (if drill-down needed)
- [ ] User detail screen with block action
- [ ] Meal edit/create screens

---

## Future Enhancements

- **Dark Mode Support:** Extend color palette to `colors-night.xml`
- **Responsive Layouts:** Multi-column dashboard for tablets
- **Advanced Charts:** Pie charts for meal distribution, heatmaps for time-based trends
- **Export Analytics:** PDF report generation with revenue summary
- **User Insights:** Drill-down into individual user payment history
- **Notifications Panel:** In-app notification history within owner panel
- **Batch Actions:** Multi-select orders/payments for bulk state changes

---

## References

- **Design System:** Material Design 3 (Material You)
- **Chart Library:** MPAndroidChart
- **Navigation:** Android Navigation Component (Fragments)
- **State Management:** ViewModel + Flow/LiveData
- **DI Framework:** Hilt

---

*Document Version:* 1.0  
*Last Updated:* April 2026  
*Audience:* Android developers, UI engineers, product designers

