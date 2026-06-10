package uw.task;

import uw.task.listener.CronerTaskListener;
import uw.task.listener.RunnerTaskListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务监听管理器。
 *
 * @author axeon
 */
public class TaskListenerManager {

    /**
     * runner监听器列表。
     */
    private final List<RunnerTaskListener> runnerListenerList = new CopyOnWriteArrayList<>();

    /**
     * croner监听器列表。
     */
    private final List<CronerTaskListener> cronerListenerList = new CopyOnWriteArrayList<>();

    /**
     * 加入一个RunnerListener。
     *
     * @param listener
     */
    public void addRunnerListener(RunnerTaskListener listener) {
        runnerListenerList.add(listener);
    }

    /**
     * 加入一个RunnerListener.
     *
     * @param listener
     */
    public void addCronerListener(CronerTaskListener listener) {
        cronerListenerList.add(listener);
    }

    /**
     * 清除RunnerListener列表
     */
    public void clearRunnerListener() {
        runnerListenerList.clear();
    }

    /**
     * 清除RunnerListener列表
     */
    public void clearCronerListener() {
        cronerListenerList.clear();
    }

    /**
     * 获取Runner Listener列表。
     *
     * @return
     */
    public List<RunnerTaskListener> getRunnerListenerList() {
        return runnerListenerList;
    }

    /**
     * 获取Croner Listener列表。
     *
     * @return
     */
    public List<CronerTaskListener> getCronerListenerList() {
        return cronerListenerList;
    }

}
