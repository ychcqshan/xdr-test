package com.xdr.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class GraphDTO {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Node {
        private String id; // IP or Hostname
        private String label;
        private String type; // ASSET or EXTERNAL
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Edge {
        private String source;
        private String target;
        private String label; // Port/Protocol
    }
}
