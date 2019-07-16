package com.aron;

import lombok.extern.log4j.Log4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * HolidayRequest
 * <p></p>
 *
 * @author aron
 * @date 2019-07-16 10:31
 */
@Log4j
public class HolidayRequest {

    private static final String HOLIDAY_REQUEST_BPMN_XML = "holiday-request.bpmn20.xml";
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/flowable?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone" +
            "=UTC&rewriteBatchedStatements=true";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "woshixuhu1217";
    public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    public static void main(String[] args) {
        ProcessEngine processEngine = buildProcessEngine();
        //deploy defined workflow
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource(HOLIDAY_REQUEST_BPMN_XML)
                .deploy();
        //query processDefinition
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());

        //input
        Scanner scanner = new Scanner(System.in);
        System.out.println("Who are you?");
        String employee = scanner.nextLine();
        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());
        System.out.println("Why do you need them?");
        String description = scanner.nextLine();
        //start process instance
        ProcessInstance processInstance = setVariablesForApply(processEngine, employee, nrOfHolidays, description);

        approveOrRejectRequestProcess(processEngine, scanner);

        holidayApprovedProcess(scanner, employee, processEngine);
        //Working with historical data
        timeConsuming(processEngine, processInstance);
    }

    private static ProcessEngine buildProcessEngine() {
        log.info("initial");
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl(JDBC_URL)
                .setJdbcUsername(USERNAME)
                .setJdbcPassword(PASSWORD)
                .setJdbcDriver(JDBC_DRIVER)
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        return cfg.buildProcessEngine();
    }

    private static ProcessInstance setVariablesForApply(ProcessEngine processEngine, String employee, Integer nrOfHolidays, String description) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        return runtimeService.startProcessInstanceByKey("holidayRequest", variables);
    }

    private static void approveOrRejectRequestProcess(ProcessEngine processEngine, Scanner scanner) {
        Map<String, Object> variables;//task query
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + ") " + tasks.get(i).getName());
        }
        //process task
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this?");

        //approve process
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);
    }

    private static void holidayApprovedProcess(Scanner scanner, String employee, ProcessEngine processEngine) {
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateOrAssigned(employee).list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + ") " + tasks.get(i).getName());
        }
        //process task
        if (!tasks.isEmpty()) {
            System.out.println("Which task would you like to complete?");
            int secondTaskIndex = Integer.valueOf(scanner.nextLine());
            Task secondTask = tasks.get(secondTaskIndex - 1);
            Map<String, Object> secondProcessVariables = taskService.getVariables(secondTask.getId());
            String result = Boolean.TRUE.equals(secondProcessVariables.get("approved")) ? "approved" : "rejected";
            System.out.println(secondProcessVariables.get("employee") + ", your " +
                    secondProcessVariables.get("nrOfHolidays") + " of holidays " + result + " by your leader. Please confirm");

            //approve process
            boolean confirmed = scanner.nextLine().toLowerCase().equals("y");
            Map<String, Object> variables = new HashMap<>();
            variables.put("confirm", confirmed);
            taskService.complete(secondTask.getId(), variables);
        }
    }

    private static void timeConsuming(ProcessEngine processEngine, ProcessInstance processInstance) {
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }
    }
}
