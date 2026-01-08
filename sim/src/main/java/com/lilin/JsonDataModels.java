package com.lilin;

import java.util.List;
import java.util.Map;

// Helper POJOs for JSON serialization/deserialization with Gson

// --- From Java to Python ---
public class JsonDataModels {

    public static class StateUpdateRequest {
        public String type = "state_update";
        public Map<String, Object> state; // Actual state data
    }

    public static class HistoryData {
        public String type = "history_data";
        public double reward;
        public boolean done;
        public Map<String, Object> next_state; // State after action applied and env stepped
    }

    public static class EpisodeInitAck {
        public String type = "episode_init_ack";
        public String message;
    }

    // --- From Python to Java ---

    public static class ActionRequest {
        public String type = "action";
        public List<Integer> action; // List of server IDs for subtasks
    }

    public static class RequestState {
        public String type = "request_state";
    }

    public static class InitEpisode {
        public String type = "init_episode";
    }
}