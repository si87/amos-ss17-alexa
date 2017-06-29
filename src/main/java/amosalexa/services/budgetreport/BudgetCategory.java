package amosalexa.services.budgetreport;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BudgetCategory {

    private String nameCategory;
    private Double limitCategory;
    private Double categoryAmountSpend;

    public BudgetCategory(String nameCategory, Double limitCategory, Double categoryAmountSpend) {
        this.nameCategory = nameCategory;
        this.limitCategory = limitCategory;
        this.categoryAmountSpend = categoryAmountSpend;
    }


    public String getAmountTotal() {
        return String.valueOf(round(categoryAmountSpend, 2));
    }

    public String getAmountPercentage() {
        int percentageSpend = (int)((categoryAmountSpend / limitCategory) * 100);
        if (percentageSpend > 100) {percentageSpend = 100;}
        return String.valueOf(percentageSpend);
    }

    public String getColorCode() {
        if (categoryAmountSpend / limitCategory > 1) {
            return "red";
        } else if (categoryAmountSpend / limitCategory > 0.7) {
            return "yellow";
        } else
        return "green";
    }

    public String getNameCategory() {
        return nameCategory;
    }

    public String getLimitCategory() {
        return String.valueOf(round(limitCategory, 2));
    }



    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
