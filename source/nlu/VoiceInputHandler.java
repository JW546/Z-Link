package nlu;

import java.util.*;

/**
 * 语音/键盘输入处理器接口
 * 支持多种输入方式（语音、键盘、文本文件等）
 * 
 * 演进：从纯文本 → 语音识别 → 多模态
 */
public interface VoiceInputHandler {
    
    /**
     * 获取输入
     */
    String getInput();
    
    /**
     * 检查是否有待处理的输入
     */
    boolean hasInput();
}

/**
 * 键盘输入处理器（立即可用）
 */
class KeyboardInputHandler implements VoiceInputHandler {
    private Scanner scanner = new Scanner(System.in);
    
    @Override
    public String getInput() {
        System.out.print("\n🎤 请输入意图（或输入'quit'退出）：");
        return scanner.nextLine().trim();
    }
    
    @Override
    public boolean hasInput() {
        return scanner.hasNextLine();
    }
}

/**
 * 语音输入处理器（演示版本，使用Web Speech API模拟）
 * 实际部署时可替换为真实语音库（如Google Speech API、科大讯飞等）
 */
class VoiceInputHandlerImpl implements VoiceInputHandler {
    
    // 模拟语音识别结果字典
    private final Map<String, String> voiceToText = new HashMap<>();
    private Queue<String> inputQueue = new LinkedList<>();
    
    public VoiceInputHandlerImpl() {
        initializeVoiceDictionary();
    }
    
    /**
     * 初始化语音模拟字典
     * 实际应用：这里应该接入真实语音识别API
     */
    private void initializeVoiceDictionary() {
        // 常见坐标点
        voiceToText.put("西大", "我要去西南大学");
        voiceToText.put("医院", "请导航到最近的医院");
        voiceToText.put("机场", "我要去机场，很急");
        voiceToText.put("火车站", "导航到火车站");
        voiceToText.put("山区", "配送医疗物资到山区");
        voiceToText.put("应急", "这是应急任务，非常紧急");
    }
    
    @Override
    public String getInput() {
        if (!inputQueue.isEmpty()) {
            return inputQueue.poll();
        }
        
        // 在实际场景中，这里应该调用语音识别API
        System.out.print("\n🎙️  正在监听语音输入... ");
        System.out.println("（演示模式：输入关键词如'西大'、'医院'等）");
        
        Scanner scanner = new Scanner(System.in);
        String voiceKeyword = scanner.nextLine().trim();
        
        if (voiceToText.containsKey(voiceKeyword)) {
            String recognizedText = voiceToText.get(voiceKeyword);
            System.out.println("🔊 语音识别结果: " + recognizedText);
            return recognizedText;
        } else {
            System.out.println("⚠️ 未识别，请重新输入或使用键盘输入");
            return null;
        }
    }
    
    @Override
    public boolean hasInput() {
        return !inputQueue.isEmpty();
    }
    
    /**
     * 预加载输入队列（用于演示）
     */
    public void preloadInputs(List<String> inputs) {
        inputQueue.addAll(inputs);
    }
}

/**
 * 批量/文件输入处理器（用于演示或测试）
 */
class BatchInputHandler implements VoiceInputHandler {
    private Queue<String> inputs = new LinkedList<>();
    
    public BatchInputHandler(List<String> inputList) {
        this.inputs.addAll(inputList);
    }
    
    @Override
    public String getInput() {
        return inputs.poll();
    }
    
    @Override
    public boolean hasInput() {
        return !inputs.isEmpty();
    }
}
