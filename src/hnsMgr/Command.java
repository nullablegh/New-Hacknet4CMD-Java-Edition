package hnsMgr;

import Exceptions.CountException;

import java.util.ArrayList;
import java.util.Arrays;

public class Command {
    public int argCount = 0;
    public final ArrayList<String> args = new ArrayList<>();
    private final String kw;
    private final Format fmt;
    public Command(String name, int argumentsCount){
        this.kw = name;
        this.argCount = argumentsCount;
        this.fmt = new Format();
    }
    public String getKwName(){
        return this.kw;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return argCount == command.argCount && java.util.Objects.equals(kw, command.kw);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(kw, argCount);
    }
    public String[] allArgs(String line){
        ArrayList<String> strings = new ArrayList<>();
        String[] strArray= fmt.split(line.trim(),' ');
        int actualArgCount = strArray.length - 1;

        if(actualArgCount != argCount && argCount != -1){
            throw new CountException("Wrong Argument Count");
        }
        if (argCount > 0) {
            strings.addAll(Arrays.asList(strArray).subList(1, argCount + 1));
        } else if (argCount == -1) {
            strings.addAll(Arrays.asList(strArray).subList(1, strArray.length));
        }
        String[] finalResult = new String[0];
        return  strings.toArray(finalResult);
    }
}
