package ssf.budgetbliss.models;

import java.util.Set;

public class Constants {

    public static final String PASSWORD = "password";
    public static final String BALANCE = "balance";
    public static final String DEF_CURR = "currency";
    public static final String TRANSACTIONS = "transactions";
    public static final String USERID = "userId";
    public static final String IN = "in";
    public static final String OUT = "out";
    
    public static final String CURR_URL = "https://api.freecurrencyapi.com/v1/currencies";
    public static final String CONVERT_URL = "https://api.freecurrencyapi.com/v1/latest";
    public static final String CHART_URL = "https://quickchart.io/chart";

    public static final Set<String> BG_COLOR = Set.of("rgb(255, 99, 132)", "rgb(75, 192, 192)", "rgb(255, 159, 64)",
                                            "rgb(255, 205, 86)", "rgb(54, 162, 235)", "rgb(54, 84, 235)",
                                            "rgb(114, 54, 235)");
   
}