package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import io.openliberty.tools.common.ai.util.Assistant;
import io.openliberty.tools.common.ai.util.ModelBuilder;

public class AgentTools implements ToolInterface {

    private ModelBuilder modelBuilder = new ModelBuilder();
    
    private String output = "";

    Assistant assistant;
    CodingTools codingTools;
    OpenLibertyTools openLibertyTools = new OpenLibertyTools();

    CodeGenerator codeGenerator;
    HumanInTheLoop humanInTheLoop;
    SupervisorAgent codeSupervisor;
    
    public AgentTools(CodingTools codingTools) {
        this.codingTools = codingTools;
        codeGenerator = AgenticServices
                        .agentBuilder(CodeGenerator.class)
                        .chatModel(modelBuilder.getChatModel())
                        .build();

        humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("An agent that asks for clarification")
                .outputName("sign")
                .requestWriter(request -> {
                    System.out.println(request);
                    System.out.print("> ");
                })
                .responseReader(() -> System.console().readLine())
                .build();

        codeSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(modelBuilder.getChatModel())
                .subAgents(codeGenerator, humanInTheLoop, new CodeReader(codingTools), new CodeWriter(codingTools))
                .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
                .build();
    }

    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }


    @Tool("Enable an openliberty feature")
    public String enableOpenLibertyFeature(@P("What you would like to do") String query) throws Exception {
        return codeSupervisor.invoke("Here are all the installable openliberty features: " + openLibertyTools.getInstallableLibertyFeatures(null)
                                        + " Can you enable the openliberty feature by updating the pom.xml and server.xml appropriately?"
                                        + " Do not assume the pom.xml and server.xml path, only input 'pom.xml' and 'server.xml' to the tools. "
                                        + " Use your agents to read the file, generate the new code, then save it. "
                                        + " If you are unable to save the new server.xml/pom.xml return why. "
                                        + " Otherwise, return what you did and explain what you did in a detailed and consise manner. "
                                        + " Here is the user's query: " + query);
    }

    public String getOutput() {
        return output;
    }

    public void flushOutput() {
        output = "";
    }
}
