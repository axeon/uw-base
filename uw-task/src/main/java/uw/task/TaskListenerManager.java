package uw.task;

import uw.task.listener.TaskCronerListener;
import uw.task.listener.TaskRunnerListener;
import uw.task.listener.TaskSendListener;

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
     * 投递监听器列表（sendToQueue 投递链路侧）。
     */
    private final List<TaskSendListener> sendListenerList = new CopyOnWriteArrayList<>();

    /**
     * 加入一个 SendListener.
     *
     * @param listener
     */
    public void addSendListener(TaskSendListener listener) {
        sendListenerList.add(listener);
    }

    /**
     * 清除 SendListener 列表
     */
    public void clearSendListener() {
        sendListenerList.clear();
    }

    /**
     * 获取 Send Listener列表.
     *
     * @return
     */
    public List<TaskSendListener> getSendListenerList() {
        return sendListenerList;
    }

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


}
