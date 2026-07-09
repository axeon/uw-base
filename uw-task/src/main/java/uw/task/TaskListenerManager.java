package uw.task;

import uw.task.listener.TaskCronerListener;
import uw.task.listener.TaskDelayerListener;
import uw.task.listener.TaskRunnerListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务监听管理器。
 *
 * @author axeon
 */
public class TaskListenerManager {

    /**
     * croner监听器列表。
     */
    private final List<TaskCronerListener> cronerListenerList = new CopyOnWriteArrayList<>();

    /**
     * runner监听器列表。
     */
    private final List<TaskRunnerListener> runnerListenerList = new CopyOnWriteArrayList<>();

    /**
     * delayer监听器列表。
     */
    private final List<TaskDelayerListener> delayerListenerList = new CopyOnWriteArrayList<>();

    /**
     * 加入一个 CronerListener.
     *
     * @param listener
     */
    public void addCronerListener(TaskCronerListener listener) {
        cronerListenerList.add(listener);
    }

    /**
     * 加入一个RunnerListener。
     *
     * @param listener
     */
    public void addRunnerListener(TaskRunnerListener listener) {
        runnerListenerList.add(listener);
    }

    /**
     * 添加一个DelayerListener。
     *
     * @param listener
     */
    public void addDelayerListener(TaskDelayerListener listener) {
        delayerListenerList.add(listener);
    }

    /**
     * 清除 CronerListener 列表
     */
    public void clearCronerListener() {
        cronerListenerList.clear();
    }

    /**
     * 清除RunnerListener列表
     */
    public void clearRunnerListener() {
        runnerListenerList.clear();
    }


    /**
     * 清除DelayerListener列表
     */
    public void clearDelayerListener() {
        delayerListenerList.clear();
    }

    /**
     * 获取Croner Listener列表。
     *
     * @return
     */
    public List<TaskCronerListener> getCronerListenerList() {
        return cronerListenerList;
    }

    /**
     * 获取Runner Listener列表。
     *
     * @return
     */
    public List<TaskRunnerListener> getRunnerListenerList() {
        return runnerListenerList;
    }

    /**
     * 获取Delayer Listener列表。
     *
     * @return
     */
    public List<TaskDelayerListener> getDelayerListenerList() {
        return delayerListenerList;
    }


}
