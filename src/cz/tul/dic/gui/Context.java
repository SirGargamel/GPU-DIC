package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public class Context {

    static {
        instance = new Context();
    }
    
    public static Context getInstance() {
        return instance;
    }

    private static final Context instance;
    private TaskContainer tc;

    private Context() {
    }

    public TaskContainer getTc() {
        return tc;
    }

    public void setTc(TaskContainer tc) {
        this.tc = tc;
    }

}
