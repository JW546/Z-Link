package model;

import generated.TailorBaseVisitor;
import generated.TailorParser;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import utils.UnitConverter;
import model.Mission;
import model.Task;
import model.PathPlan;
import model.State;
import model.AnnotationInfo;

import java.util.*;

public class DSLVisitor extends TailorBaseVisitor<Object> {
    private Mission currentMission;
    private Task currentTask;
    private PathPlan currentPath; 
    private State currentState;
    private StringBuilder generatedJavaCode = new StringBuilder();
    private List<String> constructorStatements = new ArrayList<>();
    private AnnotationInfo pendingAnnotation;

    @Override
    public Object visitTailorProgram(TailorParser.TailorProgramContext ctx) {
        try {
            List<Mission> missions = new ArrayList<>();
            for (TailorParser.TopicDeclarationContext topicCtx : ctx.topicDeclaration()) {
                Mission mission = (Mission) visitTopicDeclaration(topicCtx);
                if (mission != null) missions.add(mission);
            }
            return missions.isEmpty() ? null : missions.get(0);
        } catch (Exception e) {
            System.err.println("visitTailorProgram 中发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Mission visitTopicDeclaration(TailorParser.TopicDeclarationContext ctx) {
        try {
            String topicName = ctx.IDENTIFIER().getText();
            currentMission = new Mission(topicName);

        generatedJavaCode.append("import model.*;\n");
        generatedJavaCode.append("import annotations.*;\n\n");
        generatedJavaCode.append("public class ").append(topicName).append(" {\n");

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            
            // 直接调用具体的visit方法
            Object result = null;
            if (child instanceof TailorParser.TaskDeclarationContext) {
                result = visitTaskDeclaration((TailorParser.TaskDeclarationContext) child);
            } else if (child instanceof TailorParser.PathPlanningContext) {
                result = visitPathPlanning((TailorParser.PathPlanningContext) child);
            } else if (child instanceof TailorParser.TransportationStateContext) {
                result = visitTransportationState((TailorParser.TransportationStateContext) child);
            } else {
                result = visit(child);
            }
        }

        generatedJavaCode.append("\n    public ").append(topicName).append("() {\n");
        for (String stmt : constructorStatements) {
            generatedJavaCode.append("        ").append(stmt).append("\n");
        }
        generatedJavaCode.append("    }\n");
        generatedJavaCode.append("}\n");

        return currentMission;
        } catch (Exception e) {
            System.err.println("visitTopicDeclaration 中发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object visitAnnotation(TailorParser.AnnotationContext ctx) {
        String annotationName = ctx.IDENTIFIER().getText();
        AnnotationInfo info = new AnnotationInfo(annotationName);

        TailorParser.AnnotationValueContext valueCtx = ctx.annotationValue();
        if (valueCtx != null) {
            List<TailorParser.AnnotationFieldContext> fields = valueCtx.annotationField();
            if (fields != null && !fields.isEmpty()) {
                for (TailorParser.AnnotationFieldContext fieldCtx : fields) {
                    String fieldName = fieldCtx.IDENTIFIER().getText();
                    String fieldValue = readValue(fieldCtx.literalValue(), fieldCtx.quantityValue());
                    info.addField(fieldName, fieldValue);
                }
            } else {
                if (valueCtx.STRING() != null) {
                    info.addField("value", removeQuotes(valueCtx.STRING().getText()));
                } else if (valueCtx.IDENTIFIER() != null) {
                    info.addField("value", valueCtx.IDENTIFIER().getText());
                } else if (valueCtx.INT() != null) {
                    info.addField("value", valueCtx.INT().getText());
                } else if (valueCtx.DECIMAL() != null) {
                    info.addField("value", valueCtx.DECIMAL().getText());
                } else if (valueCtx.BOOLEAN() != null) {
                    info.addField("value", valueCtx.BOOLEAN().getText());
                }
            }
        }

        pendingAnnotation = info;
        return null;
    }

    @Override
    public Object visitTaskDeclaration(TailorParser.TaskDeclarationContext ctx) {
        try {
            for (TailorParser.AnnotationContext annotationCtx : ctx.annotation()) {
                visit(annotationCtx);
            }

            String taskName = ctx.IDENTIFIER().getText();
            currentTask = new Task(taskName);
        if (pendingAnnotation != null) {
            currentTask.setAnnotation(pendingAnnotation);
            pendingAnnotation = null;
        }
        currentMission.addTask(currentTask);

        generatedJavaCode.append("\n    public Task ").append(taskName).append(";\n");

        for (TailorParser.FieldDeclarationContext fieldCtx : ctx.fieldDeclaration()) {
            visitFieldDeclaration(fieldCtx);
        }

        constructorStatements.add(taskName + " = new Task(\"" + taskName + "\");");
        for (Map.Entry<String, String> entry : currentTask.getFields().entrySet()) {
            // 去除字符串两端的引号
            String value = entry.getValue();
            if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (value != null) {
                value = value.replace("\\", "\\\\").replace("\"", "\\\"");
            }
            constructorStatements.add(taskName + ".addField(\"" + entry.getKey() + "\", \"" + value + "\");");
        }

        currentTask = null;
        return null;
        } catch (Exception e) {
            System.err.println("visitTaskDeclaration 中发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object visitPathPlanning(TailorParser.PathPlanningContext ctx) {
        for (TailorParser.AnnotationContext annotationCtx : ctx.annotation()) {
            visit(annotationCtx);
        }

        String pathName = ctx.IDENTIFIER().getText();
        currentPath = new PathPlan(pathName);
        if (pendingAnnotation != null) {
            currentPath.setAnnotation(pendingAnnotation);
            pendingAnnotation = null;
        }
        currentMission.addPath(currentPath);

        generatedJavaCode.append("\n    public PathPlan ").append(pathName).append(";\n");

        for (TailorParser.FieldDeclarationContext fieldCtx : ctx.fieldDeclaration()) {
            visitFieldDeclaration(fieldCtx);
        }

        constructorStatements.add(pathName + " = new PathPlan(\"" + pathName + "\");");
        for (Map.Entry<String, String> entry : currentPath.getFields().entrySet()) {
            String value = entry.getValue();
            if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            constructorStatements.add(pathName + ".addField(\"" + entry.getKey() + "\", \"" + value + "\");");
        }

        currentPath = null;
        return null;
    }

    @Override
    public Object visitTransportationState(TailorParser.TransportationStateContext ctx) {
        for (TailorParser.AnnotationContext annotationCtx : ctx.annotation()) {
            visit(annotationCtx);
        }

        String stateName = ctx.IDENTIFIER().getText();
        currentState = new State(stateName);
        if (pendingAnnotation != null) {
            currentState.setAnnotation(pendingAnnotation);
            pendingAnnotation = null;
        }
        currentMission.addState(currentState);

        generatedJavaCode.append("\n    public State ").append(stateName).append(";\n");

        for (TailorParser.FieldDeclarationContext fieldCtx : ctx.fieldDeclaration()) {
            visitFieldDeclaration(fieldCtx);
        }

        constructorStatements.add(stateName + " = new State(\"" + stateName + "\");");
        for (Map.Entry<String, String> entry : currentState.getFields().entrySet()) {
            String value = entry.getValue();
            if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (value != null) {
                value = value.replace("\\", "\\\\").replace("\"", "\\\"");
            }
            constructorStatements.add(stateName + ".addField(\"" + entry.getKey() + "\", \"" + value + "\");");
        }

        currentState = null;
        return null;
    }

    @Override
    public Object visitFieldDeclaration(TailorParser.FieldDeclarationContext ctx) {
        String fieldName = ctx.fieldName != null ? ctx.fieldName.getText() : null;
        TailorParser.FieldValueContext fvCtx = ctx.fieldValue();

        String value = readFieldValue(fvCtx);

        // 确保值是有效的，避免 null 错误
        if (value != null) {
            if (currentTask != null) {
                currentTask.addField(fieldName, value);
            } else if (currentPath != null) {
                currentPath.addField(fieldName, value);
            } else if (currentState != null) {
                currentState.addField(fieldName, value);
            }
        }
        
        return null;
    }

    @Override
    public Object visitLiteralValue(TailorParser.LiteralValueContext ctx) {
        return ctx.getText();
    }

    @Override
    public Object visitQuantityValue(TailorParser.QuantityValueContext ctx) {
        return ctx.getText();
    }
    
    /**
     * 去除字符串两端的引号
     */
    private String removeQuotes(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String readFieldValue(TailorParser.FieldValueContext fvCtx) {
        if (fvCtx == null) {
            return null;
        }

        TailorParser.LiteralValueContext literalValueCtx = fvCtx.literalValue();
        if (literalValueCtx != null) {
            String text = literalValueCtx.getText();
            if (text == null) {
                return null;
            }
            if (text.startsWith("\"") && text.endsWith("\"")) {
                return removeQuotes(text);
            }
            return text;
        }

        TailorParser.QuantityValueContext quantityValueCtx = fvCtx.quantityValue();
        if (quantityValueCtx != null) {
            return quantityValueCtx.getText();
        }

        return fvCtx.getText();
    }

    private String readValue(TailorParser.LiteralValueContext literalCtx,
                             TailorParser.QuantityValueContext quantityCtx) {
        if (literalCtx != null) {
            String text = literalCtx.getText();
            if (text != null && text.startsWith("\"") && text.endsWith("\"")) {
                return removeQuotes(text);
            }
            return text;
        }
        if (quantityCtx != null) {
            return quantityCtx.getText();
        }
        return null;
    }

    public String getGeneratedJavaCode() {
        return generatedJavaCode.toString();
    }
}