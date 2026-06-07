package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import utils.UnitConverter;

public class Mission {
    private String name;
    private List<Task> tasks = new ArrayList<>();
    private List<PathPlan> paths = new ArrayList<>();
    private List<State> states = new ArrayList<>();

    public Mission(String name) {
        this.name = name;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void addPath(PathPlan path) {
        paths.add(path);
    }

    public void addState(State state) {
        states.add(state);
    }

    public String getName() {
        return name;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public List<PathPlan> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public List<State> getStates() {
        return Collections.unmodifiableList(states);
    }

    @Override
    public String toString() {
        return "Mission{name='" + name + "', tasks=" + tasks.size() + 
               ", paths=" + paths.size() + ", states=" + states.size() + "}";
    }
}