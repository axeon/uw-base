package uw.task.listener;

import uw.task.TaskData;

/**
 * Runner任务的监听器。
 *
 * @author axeon
 */
public interface RunnerTaskListener {

    /**
     * 执行前的监听器。
     *
     * @param data
     */
    void onPreExecute(TaskData data);

    /**
     * 执行后的监听器。
     *
     * @param data
     */
    void onPostExecute(TaskData data);

}
