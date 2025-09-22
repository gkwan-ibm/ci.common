/**
 * (C) Copyright IBM Corporation 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.common.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import io.openliberty.tools.common.ai.tools.AgentTools;
import io.openliberty.tools.common.ai.tools.CodingTools;
import io.openliberty.tools.common.ai.tools.OpenLibertyTools;
import io.openliberty.tools.common.ai.tools.StackOverFlowTools;
import io.openliberty.tools.common.ai.tools.ToolInterface;
import io.openliberty.tools.common.ai.util.Assistant;
import io.openliberty.tools.common.ai.util.MarkdownConsoleFormatter;
import io.openliberty.tools.common.ai.util.ModelBuilder;
import io.openliberty.tools.common.ai.util.RagCreator;
import io.openliberty.tools.common.ai.util.Utils;
import jakarta.validation.constraints.Null;

public class ChatAgent {
    private ModelBuilder modelBuilder = new ModelBuilder();

    private ArrayList <ToolInterface> tools;

    private MarkdownConsoleFormatter mdFormatter = new MarkdownConsoleFormatter();

    private Assistant assistant = null;

    private int memoryId;

    private boolean toolsEnabled = false;

    public ChatAgent(int memoryId) throws Exception {
        this(memoryId, new CodingTools());
    }

    public ChatAgent(int memoryId, CodingTools codingTools) throws Exception {
        this.memoryId = memoryId;

        AgentTools agentTools = new AgentTools(codingTools);
        this.tools = new ArrayList<>(Arrays.asList(codingTools, new StackOverFlowTools(), new OpenLibertyTools(), agentTools));

        this.assistant = getAssistant();

        agentTools.setAssistant(this.assistant);
    }

    public Assistant getAssistant() throws Exception {
        if (assistant == null) {
            AiServices<Assistant> builder = null;
            try {
                builder = AiServices.builder(Assistant.class)
                    .chatModel(modelBuilder.getChatModel())
                    .tools(this.tools.toArray())
                    .hallucinatedToolNameStrategy(
                        toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,
                            "Error: there is no tool with the following parameters called "
                            + toolExecutionRequest.name()))
                    .chatMemoryProvider(
                         sessionId -> MessageWindowChatMemory.withMaxMessages(modelBuilder.getMaxMessages()));
                
            }  catch (Exception e) {
                e.printStackTrace();
            }
            RagCreator creator = new RagCreator();
            RetrievalAugmentor retrivalAugmentator = creator.getRetrievalAugmentor(modelBuilder.getEmbeddingModel());
            if (retrivalAugmentator == null) {
                System.out.println("[WARNING] RAG is not set up successfully. Continuing without RAG.");
            } else {
                builder.retrievalAugmentor(retrivalAugmentator);
            }

            assistant = builder.build();

            try {
                assistant.chat(memoryId, "test");
                toolsEnabled = true;
            } catch (InvalidRequestException e) {
                toolsEnabled = false;
                if (e.getMessage().contains("does not support tools")) {
                    System.out.println("WARNING: AI model " +
                        modelBuilder.getModelName() + " does not support tools");
                    builder = AiServices.builder(Assistant.class)
                        .chatModel(modelBuilder.getChatModel())
                        .chatMemoryProvider(
                            sessionId -> MessageWindowChatMemory.withMaxMessages(modelBuilder.getMaxMessages()));
                    if (retrivalAugmentator != null) {
                        builder.retrievalAugmentor(retrivalAugmentator);
                    }
                    assistant = builder.build();
                } else {
                    throw new Exception(e);
                }
            } finally {
                resetChat();
            }
        }
        return assistant;
    }

    public String chat(String message) throws Exception {
        if (message.equalsIgnoreCase("reset")) {
            resetChat();
            return "The current chat session is reset.\n";
        } else {
            String response = getAssistant().chat(memoryId, message).content().trim();

            for (ToolInterface tool : tools) {
                if (!tool.getOutput().isBlank()) {
                    response = tool.getOutput();
                    tool.flushOutput();
                }
            }

            return mdFormatter.rerender(response);
        }
    }

    public void resetChat() {
        assistant.evictChatMemory(memoryId);
        Utils.clearPermissions();
    }

    public String getModelName() {
        return modelBuilder.getModelName();
    }

    public String getProvider() {
        return modelBuilder.getProvider();
    }

    public String getToolsEnabled() {
        return toolsEnabled ? "enabled" : "unavailable";
    }

    public Integer getTimeOut() {
        return modelBuilder.getTimeOut();
    }

    public Integer getMaxNewToken() {
        return modelBuilder.getMaxNewToken();
    }

    public Double getTemperature() {
        return modelBuilder.getTemperature();
    }

}
