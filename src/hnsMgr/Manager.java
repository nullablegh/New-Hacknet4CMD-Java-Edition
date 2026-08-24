package hnsMgr;

import java.util.*;

public class Manager {
    private HashMap<String,IExecute> cmd = new HashMap<>();
    private final HashMap<String, String> variables = new HashMap<>();
    public void add(String command, IExecute execute){
        cmd.put(command,execute);
    }
    public void run(String kw, String[] args) {
        IExecute executeInstance = cmd.get(kw);
        if (executeInstance != null) {
            executeInstance.execute(args); // 把參數傳進去
        } else {
            System.out.println("Unknown command: " + kw);
        }
    }
    public void setVariable(String name, String value) {
        variables.put(name, value);
    }
    public String getVariable(String name) {
        String result = name;
        for (String key : variables.keySet()) {
            String target = "%" + key + "%";
            if (result.contains(target)) {
                result = result.replace(target, variables.get(key));
            }
        }
        return result;
    }
    public java.util.Set<String> getVariableKeys() {
        return variables.keySet();
    }

    public String getRawVariable(String key) {
        return variables.getOrDefault(key, "");
    }
}
