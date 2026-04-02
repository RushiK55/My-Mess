# Owner Panel Design – Visual Reference & Example Code

## Color Palette Reference

### Primary Colors
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ■ Owner Primary: #2C3E66 (Royal Blue)
    └─ Usage: Headings, primary text, main bars in charts
    └─ Hex: #2C3E66 | RGB: (44, 62, 102)

  ■ Owner Accent: #F39C12 (Golden Amber)
    └─ Usage: Action buttons (Accept, Approve, Mark Paid), highlights
    └─ Hex: #F39C12 | RGB: (243, 156, 18)

  ■ Owner Secondary: #6C7A89 (Steel Gray)
    └─ Usage: Body text, borders, secondary information
    └─ Hex: #6C7A89 | RGB: (108, 122, 137)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Background & Surface Colors
```
  ■ Owner Background: #ECF0F1 (Off-White)
    └─ Usage: Screen background, reduces eye strain
    └─ Hex: #ECF0F1 | RGB: (236, 240, 241)

  ■ Owner Card Background: #FFFFFF (White)
    └─ Usage: Card surfaces, content containers
    └─ Hex: #FFFFFF | RGB: (255, 255, 255)

  ■ Owner Card Border: #D5DDE5 (Light Gray)
    └─ Usage: Card outlines, dividers (1dp stroke)
    └─ Hex: #D5DDE5 | RGB: (213, 221, 229)
```

### Semantic Colors
```
  ■ Owner Success: #2E7D32 (Green)
    └─ Usage: Positive states, accepted orders, confirmed actions
    └─ Hex: #2E7D32 | RGB: (46, 125, 50)

  ■ Owner Info: #2C7FB8 (Info Blue)
    └─ Usage: Informational states, secondary data, pending items
    └─ Hex: #2C7FB8 | RGB: (44, 127, 184)

  ■ Owner Danger: #B23A48 (Red)
    └─ Usage: Error states, rejected items, warnings
    └─ Hex: #B23A48 | RGB: (178, 58, 72)
```

---

## Typography Scale

```
┌──────────────────────────────────────────────────────────────────┐
│ HEADING (TextAppearance.MyMess.Owner.Heading)                   │
│ Size: 22sp  │  Font: sans-serif  │  Weight: bold                │
│ Color: owner_primary (#2C3E66)                                  │
│ Used for: Page titles like "Owner Dashboard"                    │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ Section Title (TextAppearance.MyMess.Owner.SectionTitle)        │
│ Size: 18sp  │  Font: sans-serif  │  Weight: bold                │
│ Color: owner_primary (#2C3E66)                                  │
│ Used for: Section headers like "Requests Queue"                 │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ Body (TextAppearance.MyMess.Owner.Body)                         │
│ Size: 15sp  │  Font: sans-serif  │  Weight: regular             │
│ Color: owner_secondary (#6C7A89)                                │
│ Used for: Descriptions, labels, list item details               │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ Data (TextAppearance.MyMess.Owner.Data)                         │
│ Size: 20sp  │  Font: sans-serif-medium  │  Weight: bold         │
│ Color: owner_primary (#2C3E66)                                  │
│ Used for: Metric values like "148 users", "Rs 2,450"           │
└──────────────────────────────────────────────────────────────────┘
```

---

## Component Examples

### Dashboard Metric Card
```
┌─────────────────────────────────────┐
│  Enrolled Users      [Card border]  │
│  248                                │
└─────────────────────────────────────┘
    ↑ "Enrolled Users" = Body style, owner_secondary
    ↑ "248" = Data style, owner_primary (22sp bold)
    ↑ Card: 8dp radius, white bg, gray border
```

### Accept/Reject Buttons
```
┌──────────────┐  ┌──────────────┐
│   Accept     │  │   Reject     │
└──────────────┘  └──────────────┘
   ↑ Gold (#F39C12)  ↑ Outlined, Gray text
   ↑ White text       ↑ owner_secondary border
   ↑ ButtonAccent style ↑ ButtonOutline style
```

### Order Card Item
```
┌──────────────────────────────────────┐
│ Biryani (3x)      [Title style]      │
│ Order #3452 | Pending...  [Body]     │
│ Status: Preparing...      [Info blue]│
│                                      │
│ ┌─────────────┐                      │
│ │   Advance   │ [ButtonAccent]       │
│ └─────────────┘                      │
└──────────────────────────────────────┘
   ↑ Widget.MyMess.Owner.Card
```

### Join Request Card
```
┌──────────────────────────────────────┐
│ Rajesh Kumar      [SectionTitle]     │
│ rajesh@email.com  [Body style]       │
│                                      │
│ ┌─────────────┐ ┌──────────────────┐│
│ │   Accept    │ │    Reject        ││
│ └─────────────┘ └──────────────────┘│
└──────────────────────────────────────┘
   ↑ ButtonAccent (Accept)
   ↑ ButtonOutline (Reject)
```

### Chart in Analytics
```
┌──────────────────────────────────────┐
│  [Owner Card Style]                  │
│                                      │
│  ┌─────────────────────────────────┐ │
│  │  ▓▓▓                             │ │
│  │  ▓▓▓  ▓▓▓▓    ▓▓▓▓              │ │
│  │  ▓▓▓  ▓▓▓▓▓  ▓▓▓▓▓  ▓▓▓▓       │ │
│  │  Mon Tue  Wed  Thu  Fri         │ │
│  │  Orders per day                 │ │
│  └─────────────────────────────────┘ │
│  ↑ Bars: owner_primary (#2C3E66)     │
│  ↑ Text: owner_secondary (#6C7A89)   │
└──────────────────────────────────────┘
```

---

## Layout Structure Example: Dashboard

```
┌──────────────────────────────────────────────────────┐
│ Screen Background: #ECF0F1 (owner_bg)                │
├──────────────────────────────────────────────────────┤
│                                                       │
│  Owner Dashboard                    [Heading 22sp]   │
│  Business snapshot and active requests [Body 15sp]   │
│                                                       │
│  ┌──────────────────────────────────────────────────┐ │
│  │ [Carousel/Banners - 150dp]                       │ │
│  └──────────────────────────────────────────────────┘ │
│                                                       │
│  ┌───────┐ ┌───────┐ ┌───────┐   [3 metric cards]   │
│  │ 248   │ │ 12    │ │Rs 4.2K│   [layout_weight=1]  │
│  │Enroll │ │Pending│ │Earnings  [Card style]        │
│  └───────┘ └───────┘ └───────┘                       │
│                                                       │
│  Requests Queue              [SectionTitle 18sp]     │
│  ──────────────  [Divider]                           │
│                                                       │
│  [TabLayout: Order Requests | Join Requests]         │
│  [Tab Indicator: owner_accent]                       │
│                                                       │
│  [ViewPager with order/join request cards]           │
│                                                       │
│  ┌──────────────────────────────────────────────────┐ │
│  │ [BottomNavigationView]                           │ │
│  │ Home | Orders | Meals | Profile                  │ │
│  │ Icons: owner_primary, Active: owner_accent       │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## Code Snippets for Common Tasks

### Apply Heading Style to TextView
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Owner Dashboard"
    android:textAppearance="@style/TextAppearance.MyMess.Owner.Heading" />
```

### Create a Metric Card
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.MyMess.Owner.Card"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1"
    android:layout_marginEnd="6dp">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">
        <TextView
            android:text="Enrolled Users"
            android:textAppearance="@style/TextAppearance.MyMess.Owner.Body" />
        <TextView
            android:text="248"
            android:layout_marginTop="4dp"
            android:textAppearance="@style/TextAppearance.MyMess.Owner.Data" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### Style an Accept Button
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnAccept"
    style="@style/Widget.MyMess.Owner.ButtonAccent"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Accept" />
```

### Style a Chart (Kotlin)
```kotlin
// In OwnerAnalyticsFragment.kt
private fun setupCharts() {
    // Revenue line chart
    val entries = listOf(
        Entry(0f, 1000f), Entry(1f, 1200f), 
        Entry(2f, 950f), Entry(3f, 1400f)
    )
    val revenueSet = LineDataSet(entries, "Revenue")
    OwnerChartStyler.styleLineDataSet(revenueSet)
    
    binding.chartRevenue.data = LineData(revenueSet)
    OwnerChartStyler.styleBaseChart(binding.chartRevenue)
    binding.chartRevenue.invalidate()
}
```

### Create a Section Header with Divider
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:layout_marginBottom="8dp"
    android:orientation="vertical">
    <TextView
        android:text="Requests Queue"
        android:textAppearance="@style/TextAppearance.MyMess.Owner.SectionTitle" />
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="8dp"
        android:background="@color/owner_card_border" />
</LinearLayout>
```

### Use Search Field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Search by name or email"
    app:boxBackgroundColor="@color/owner_card_bg"
    app:boxStrokeColor="@color/owner_secondary">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etSearch"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>
```

---

## Design Decision Rationale

### Why Royal Blue (#2C3E66) for Primary?
- Conveys authority, trustworthiness, and professionalism
- Excellent contrast against white (7.5:1 ratio)
- Recognizable to business users worldwide

### Why Golden Amber (#F39C12) for Accent?
- Warm, approachable color that stands out without aggression
- Typically associated with positivity and approval
- Complements blue for good contrast
- Eye-catching for call-to-action buttons

### Why Card-Based Design?
- Provides clear visual separation of content
- Improves scannability of list items
- Professional, modern appearance
- Works well on all screen sizes

### Why Material Design 3?
- Built-in accessibility compliance
- Responsive animations and interactions
- System fonts for performance
- Consistent with modern Android standards

---

## Testing Checklist

- [ ] Verify all text meets minimum 14sp size
- [ ] Check button touch targets are 48dp minimum
- [ ] Confirm color contrast ratios (WCAG AA: 4.5:1 minimum)
- [ ] Test with system dark mode (future enhancement)
- [ ] Verify layout scaling on tablets
- [ ] Test navigation and transitions
- [ ] Validate strings in different languages (if applicable)
- [ ] Check loading states and empty states
- [ ] Test chart rendering on various data sizes

---

## Migration Guide (Existing Code)

If you have existing Owner screens using generic Material Design colors:

### Before:
```xml
<TextView android:textAppearance="@style/TextAppearance.MaterialComponents.Headline6" />
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button" />
```

### After:
```xml
<TextView android:textAppearance="@style/TextAppearance.MyMess.Owner.Heading" />
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MyMess.Owner.ButtonAccent" />
```

---

## Troubleshooting

### Issue: Colors don't match design
→ Verify you're using the exact hex codes from `colors.xml`

### Issue: Text size inconsistent
→ Use only the defined `TextAppearance.MyMess.Owner.*` styles

### Issue: Buttons styling not applied
→ Check that you're using `style="@style/Widget.MyMess.Owner.*"` attribute

### Issue: Charts colors off
→ Call `OwnerChartStyler` methods in Fragment code, not just layout XML

---

*Last Updated: April 2026*  
*Design System Version: 1.0*  
*Status: Production Ready*

