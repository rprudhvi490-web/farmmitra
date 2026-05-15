package com.weekendbasket.app.statemachine;

import java.util.List;

public class StateTransition {
    private String entity;
    private String from;
    private List<String> to;
    private String action;
    private String sideEffect;

    public String getEntity()             { return entity; }
    public void setEntity(String entity)  { this.entity = entity; }
    public String getFrom()               { return from; }
    public void setFrom(String from)      { this.from = from; }
    public List<String> getTo()           { return to; }
    public void setTo(List<String> to)    { this.to = to; }
    public String getAction()             { return action; }
    public void setAction(String action)  { this.action = action; }
    public String getSideEffect()                   { return sideEffect; }
    public void setSideEffect(String sideEffect)    { this.sideEffect = sideEffect; }
}
