package com.lilin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaClient {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private List<Double> AvgResults;

    public JavaClient() {
        this.gson = new GsonBuilder().create();
    }

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        System.out.println("Connected to Python server at " + ip + ":" + port);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        System.out.println("Disconnected from Python server.");
    }

    private <T> void sendJson(T data) {
        String json = gson.toJson(data);
        out.println(json);
        out.flush(); // Ensure data is sent immediately
    }

    private <T> T receiveJson(Class<T> type) throws IOException {
        String jsonLine;
        while ((jsonLine = in.readLine()) != null) {
            if (!jsonLine.trim().isEmpty()) {
                // System.out.println("Received: " + jsonLine); // Debug
                return gson.fromJson(jsonLine, type);
            }
        }
        return null; // Connection closed or empty stream
    }

    public List<Double> getAvgResults() {
        return AvgResults;
    }

    public void runSimulation(int maxEpisodes) {
        AvgResults = new ArrayList<>();

        // Generate EdgeCloudsim Scenario Factory
        ScenarioFactory sampleFactory = new LScenarioFactory();

        // Generate EdgeCloudSim Simulation Manager
        SimManager manager = new SimManager(sampleFactory);

        manager.setEdgeDeviceGeneratorModel();

        manager.setLoadGeneratorModel();

        manager.setNetworkModel();

        manager.setScheduler();

        manager.startDevice();

        try {
            startConnection(ServerConfig.PYTHON_HOST, ServerConfig.PYTHON_PORT);

            for (int episode = 0; episode < maxEpisodes; episode++) {
                System.out.println("\n--- Java Simulation: Episode " + episode + " ---");

                // 6. Java端初始化状态。
                // Wait for init_episode command from Python
                JsonDataModels.InitEpisode initRequest = receiveJson(JsonDataModels.InitEpisode.class);
                if (initRequest == null || !initRequest.type.equals("init_episode")) {
                    System.err.println("Expected 'init_episode' from Python, got: " + initRequest);
                    break;
                }
                manager.updateApp(); // Reset environment for new episode
                double diff = 0;
                manager.lastAvaCompleteTim = 0;
                manager.avaCompleteTim=0;
                manager.clearResult();
                manager.startSentApp();
                sendJson(new JsonDataModels.EpisodeInitAck()); // Acknowledge initialization

                // 9. Java端将当前状态发送给Python端。
                // Wait for state_request command from Python
                JsonDataModels.RequestState stateReq = receiveJson(JsonDataModels.RequestState.class);
                if (stateReq == null || !stateReq.type.equals("request_state")) {
                    System.err.println("Expected 'request_state' from Python, got: " + stateReq);
                    break;
                }

                JsonDataModels.StateUpdateRequest initState = new JsonDataModels.StateUpdateRequest();
                initState.state = manager.getState(diff);
                sendJson(initState);
                // System.out.println("Sent state to Python."); // Debug

                for (int t = 0; t < manager.getLoadGeneratorModel().getApp_num(); t++) {

                    // 11. Python端将action发送给Java端，Java端收到action后将当前任务调度到对应的服务器执行。
                    JsonDataModels.ActionRequest actionRequest = receiveJson(JsonDataModels.ActionRequest.class);
                    if (actionRequest == null || !actionRequest.type.equals("action")) {
                        System.err.println("Expected 'action' from Python, got: " + actionRequest);
                        break;
                    }
                    // System.out.println("Received action from Python: " + actionRequest.action); // Debug

                    // Convert List<Integer> to int[] for environment
                    int[] action = actionRequest.action.stream().mapToInt(Integer::intValue).toArray();
                    System.out.println(Arrays.toString(action));
                    double[] res = manager.applyAction(action); // Apply the action to the environment

                    // 12. Java端获取reward并进入下一个状态。
                    double reward = manager.getReward() - res[1]; // Maximize this difference
                    if (manager.lastAvaCompleteTim == 0) reward = 0; // No previous avg completion time to compare to

                    boolean episodeDone = false;
                    if(t == manager.getLoadGeneratorModel().getApp_num() - 1){
                        episodeDone = true; // Episode done if max timesteps reached
                        try {
                            // 等待剩余app完成
                            manager.wait_complete.await();
                            System.out.println("所有app完成");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // 13. Java端将此次获取的历史数据传给Python端，Python端将其保存到replaybuff中。
                    JsonDataModels.HistoryData historyData = new JsonDataModels.HistoryData();
                    historyData.reward = reward;
                    historyData.done = episodeDone;
                    historyData.next_state = manager.getState(res[0]); // This is S_t+1
                    sendJson(historyData);
                    // System.out.println("Sent history data to Python. Reward: " + reward); // Debug

                    if (episodeDone) {
                        System.out.println("Java Simulation: Episode " + episode + " finished at max timesteps.");
                        break;
                    }
                }
                if((episode+1) % 50 ==0) {
                    AvgResults.add(manager.avaCompleteTim);
                    System.out.println(manager.avaCompleteTim);
                }
            }
        } catch (IOException e) {
            System.err.println("Client IO Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                stopConnection();
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
