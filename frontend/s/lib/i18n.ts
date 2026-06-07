import type { GuideStep, HeroSlide, LocaleCode } from "../types";

export const localeMeta: Record<LocaleCode, { label: string; speechLang: string }> = {
  "zh-CN": { label: "简体中文", speechLang: "zh-CN" },
  "en-US": { label: "English", speechLang: "en-US" }
};

export const t = <T extends Record<LocaleCode, string>>(locale: LocaleCode, copy: T) => copy[locale];

export const brandName: Record<LocaleCode, { cn: string; en: string; tagline: string }> = {
  "zh-CN": {
    cn: "智通无阻",
    en: "Z-Link",
    tagline: "空地协同意图网络平台"
  },
  "en-US": {
    cn: "智通无阻",
    en: "Z-Link",
    tagline: "Air-Ground Intent Networking Platform"
  }
};

export const assistantGreeting: Record<LocaleCode, string> = {
  "zh-CN": "小智已就绪。",
  "en-US": "XiaoZhi is ready."
};

export const starterTemplates: Record<LocaleCode, string[]> = {
  "zh-CN": [
    "我要尽快去西南大学附属医院，路上如果拥堵就让无人机看一下前方，帮我选最快路线。",
    "前面看起来有点堵，帮我问问后车和无人机，选择更稳的路线。",
    "我要去江北机场，优先安全，但如果有畅通路线可以提醒我。"
  ],
  "en-US": [
    "I need to get to Southwest University Affiliated Hospital as quickly as possible. If traffic is heavy, ask the drone to look ahead and choose the fastest route.",
    "It looks congested ahead. Ask the following car and drone for advice, then choose a steadier route.",
    "I need to go to Jiangbei Airport. Prioritize safety, but notify me if a clear route appears."
  ]
};

export const defaultMissionInput: Record<LocaleCode, string> = {
  "zh-CN": starterTemplates["zh-CN"][0],
  "en-US": starterTemplates["en-US"][0]
};

export const heroSlidesByLocale: Record<LocaleCode, HeroSlide[]> = {
  "zh-CN": [
    {
      id: "city-01",
      imageUrl: "/carousel/slide-city-01.jpg",
      imageAlt: "城市夜景航拍"
    },
    {
      id: "drone-02",
      imageUrl: "/carousel/slide-drone-02.jpg",
      imageAlt: "城市上空无人机视角"
    },
    {
      id: "city-03",
      imageUrl: "/carousel/slide-city-03.jpg",
      imageAlt: "高密度城市道路网络"
    }
  ],
  "en-US": [
    {
      id: "city-01",
      imageUrl: "/carousel/slide-city-01.jpg",
      imageAlt: "Night aerial city view"
    },
    {
      id: "drone-02",
      imageUrl: "/carousel/slide-drone-02.jpg",
      imageAlt: "Drone view over the city"
    },
    {
      id: "city-03",
      imageUrl: "/carousel/slide-city-03.jpg",
      imageAlt: "Dense urban road network"
    }
  ]
};

export const guideSteps: Record<LocaleCode, GuideStep[]> = {
  "zh-CN": [
    {
      id: "welcome",
      title: "从左侧开始",
      description: "左侧导航负责页面切换。",
      hint: "任务工作台是主入口。"
    },
    {
      id: "compose",
      title: "先写任务",
      description: "输入任务后生成协同方案。",
      hint: "也可以直接选择模板。"
    },
    {
      id: "assistant",
      title: "小智在右侧",
      description: "小智会同步当前任务状态。",
      hint: "头像和资料可在用户中心调整。"
    },
    {
      id: "manage",
      title: "地图和主题在下方",
      description: "地图、时间线和 DDS 主题集中在工作台下半区。",
      hint: "需要更大视图时可使用地图工具栏。"
    }
  ],
  "en-US": [
    {
      id: "welcome",
      title: "Start from the left rail",
      description: "The left navigation handles page switching.",
      hint: "Command Center is the main entry."
    },
    {
      id: "compose",
      title: "Write the task first",
      description: "Generate a coordination plan from the mission input.",
      hint: "You can also start from a preset template."
    },
    {
      id: "assistant",
      title: "XiaoZhi stays on the right",
      description: "XiaoZhi keeps up with the current task state.",
      hint: "Avatar and account details can be updated in Users."
    },
    {
      id: "manage",
      title: "Map and topics stay below",
      description: "The map, timeline, and DDS topics are grouped under the workbench.",
      hint: "Use the map toolbar when you need a larger view."
    }
  ]
};
