package org.bonitasoft.engine.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.EventCriterion;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.IntermediateCatchEventInstance;
import org.bonitasoft.engine.bpm.flownode.TimerType;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceCriterion;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.test.TestStates;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.Test;

public class TimerEventTest extends AbstractEventTest {

    @Cover(classes = EventInstance.class, concept = BPMNConcept.EVENTS, keywords = { "Event", "Timer event", "Intermediate catch event", "User task" }, story = "Execute process with an intermediate catch event with a timer duration type.", jira = "")
    @Test
    public void timerIntermediateCatchEventDuration() throws Exception {
        final String step1Name = "step1";
        final String step2Name = "step2";
        final Expression timerExpression = new ExpressionBuilder().createConstantLongExpression(1000); // the timer intermediate catch event will wait one
                                                                                                       // second
        final ProcessDefinition definition = deployAndEnableProcessWithIntermediateCatchTimerEventAndUserTask(TimerType.DURATION, timerExpression, step1Name,
                step2Name);

        final ProcessInstance processInstance = getProcessAPI().startProcess(definition.getId());
        final ActivityInstance userTask = waitForUserTask(step1Name, processInstance);
        assignAndExecuteStep(userTask, getIdentityAPI().getUserByUserName(USERNAME).getId());

        waitForEventInWaitingState(processInstance, "intermediateCatchEvent");
        final long processInstanceId = processInstance.getId();
        EventInstance eventInstance = getEventInstance(processInstanceId, "intermediateCatchEvent");
        checkIntermediateCatchEventInstance(eventInstance, "intermediateCatchEvent", TestStates.WAITING);
        // wait trigger activation
        int cnt = 0;

        // BS-9586 : for mysql, we wait longer
        while (cnt < 10 && eventInstance != null) {
            Thread.sleep(1000);
            eventInstance = getEventInstance(processInstanceId, "intermediateCatchEvent");
            cnt++;
        }
        assertNull(eventInstance);// finished

        waitForUserTask(step2Name, processInstance);

        disableAndDeleteProcess(definition);
    }

    @Cover(classes = EventInstance.class, concept = BPMNConcept.EVENTS, keywords = { "Event", "Timer event", "Intermediate catch event", "User task" }, story = "Execute process with an intermediate catch event with a timer date type.", jira = "")
    @Test
    public void timerIntermediateCatchEventDate() throws Exception {
        final String step1Name = "step1";
        final String step2Name = "step2";
        final long expectedDate = System.currentTimeMillis() + 5000;
        final Expression timerExpression = new ExpressionBuilder().createGroovyScriptExpression("testTimerIntermediateCatchEventDate", "return new Date("
                + expectedDate + "l)", Date.class.getName()); // the timer intermediate catch
        // event will wait one second
        final ProcessDefinition definition = deployAndEnableProcessWithIntermediateCatchTimerEventAndUserTask(TimerType.DATE, timerExpression, step1Name,
                step2Name);

        final ProcessInstance processInstance = getProcessAPI().startProcess(definition.getId());
        final ActivityInstance userTask = waitForUserTask(step1Name, processInstance);
        assertNotNull(userTask);

        assignAndExecuteStep(userTask, getIdentityAPI().getUserByUserName(USERNAME).getId());

        waitForFlowNodeInState(processInstance, "intermediateCatchEvent", TestStates.WAITING, true);
        final EventInstance eventInstance = getEventInstance(processInstance.getId(), "intermediateCatchEvent");
        checkIntermediateCatchEventInstance(eventInstance, "intermediateCatchEvent", TestStates.WAITING);
        // wait trigger activation
        waitForUserTask(step2Name, processInstance);
        final long now = System.currentTimeMillis();
        assertTrue("Event has triggered too early !" + (now - expectedDate), expectedDate <= now);

        disableAndDeleteProcess(definition);
    }

    @Cover(classes = EventInstance.class, concept = BPMNConcept.EVENTS, keywords = { "Event", "Timer event", "Start event", "User task" }, story = "Execute a process with a start event with a timer date type.", jira = "")
    @Test
    public void timerStartEventDate() throws Exception {
        final String stepName = "step1";
        final long expectedDate = System.currentTimeMillis() + 1000;
        final Expression timerExpression = new ExpressionBuilder().createGroovyScriptExpression("testTimerStartEventDate", "return new Date(" + expectedDate
                + "l);", Date.class.getName()); // the new instance must be
        // created in one second
        final ProcessDefinition definition = deployAndEnableProcessWithStartTimerEventAndUserTask(TimerType.DATE, timerExpression, stepName);

        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.CREATION_DATE_DESC);
        assertTrue(processInstances.isEmpty());

        // wait for process instance creation
        waitForUserTask(stepName);

        disableAndDeleteProcess(definition);
    }

    @Cover(classes = EventInstance.class, concept = BPMNConcept.EVENTS, keywords = { "Event", "Timer event", "Start event", "User task" }, story = "Execute a process with a start event with a timer cycle type.", jira = "")
    @Test
    public void timerStartEventCycle() throws Exception {
        final Expression timerExpression = new ExpressionBuilder().createConstantStringExpression("*/4 * * * * ?"); // new instance created every 3 seconds
        final String stepName = "step1";
        final ProcessDefinition definition = deployAndEnableProcessWithStartTimerEventAndUserTask(TimerType.CYCLE, timerExpression, stepName);

        List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.CREATION_DATE_DESC);
        // the job will execute the first time at when the second change. If this arrive just after the schedule the instance can already be created
        assertTrue("There should be between 0 and 1 process, but was <" + processInstances.size() + ">",
                1 == processInstances.size() || 0 == processInstances.size());

        // wait for process instance creation
        Thread.sleep(4500);

        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.CREATION_DATE_DESC);
        assertTrue("There should be between 1 and 2 process, but was <" + processInstances.size() + ">",
                processInstances.size() >= 1 && processInstances.size() <= 2);

        // wait for process instance creation
        Thread.sleep(4500);

        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.CREATION_DATE_DESC);
        assertTrue("There should be between 2 and 3 process, but was <" + processInstances.size() + ">",
                processInstances.size() >= 2 && processInstances.size() <= 3);

        // wait for process instance creation
        Thread.sleep(4500);

        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.CREATION_DATE_DESC);
        assertTrue("There should be between 3 and 4 process, but was <" + processInstances.size() + ">",
                processInstances.size() >= 3 && processInstances.size() <= 4);

        waitForUserTask(stepName, processInstances.get(processInstances.size() - 1));
        disableAndDeleteProcess(definition);
    }

    @Cover(classes = EventInstance.class, concept = BPMNConcept.EVENTS, keywords = { "Event", "Timer event", "Start event", "User task" }, story = "Execute a process with a start event with a timer duration type.", jira = "")
    @Test
    public void timerStartEventDuration() throws Exception {
        final String stepName = "step1";
        final Expression timerExpression = new ExpressionBuilder().createConstantLongExpression(1500); // the new instance must be created in one second
        final ProcessDefinition definition = deployAndEnableProcessWithStartTimerEventAndUserTask(TimerType.DURATION, timerExpression, stepName);
        waitForInitializingProcess();

        waitForUserTask(stepName);
        disableAndDeleteProcess(definition);
    }

    private EventInstance getEventInstance(final long processInstanceId, final String eventName) throws RetrieveException {
        final List<EventInstance> eventInstances = getProcessAPI().getEventInstances(processInstanceId, 0, 10, EventCriterion.NAME_ASC);
        EventInstance searchedEventInstance = null;
        final Iterator<EventInstance> iterator = eventInstances.iterator();
        while (iterator.hasNext() && searchedEventInstance == null) {
            final EventInstance eventInstance = iterator.next();
            if (eventInstance.getName().equals(eventName)) {
                searchedEventInstance = eventInstance;
            }
        }
        return searchedEventInstance;
    }

    private void checkIntermediateCatchEventInstance(final EventInstance eventInstance, final String eventName, final TestStates state) {
        assertTrue(eventInstance instanceof IntermediateCatchEventInstance);
        checkEventInstance(eventInstance, eventName, state);
    }

    private void checkEventInstance(final EventInstance eventInstance, final String eventName, final TestStates state) {
        assertEquals(eventName, eventInstance.getName());
        assertEquals(state.getStateName(), eventInstance.getState());
    }

}
