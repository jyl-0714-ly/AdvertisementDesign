package com.advertisementdesign.back.project.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DeterministicValidRequirementPolicy implements ValidRequirementPolicy {
    private static final Set<String> GREETINGS = Set.of(
            "你好", "您好", "在吗", "嗨", "哈喽", "hello", "hi", "早上好", "下午好", "晚上好");
    private static final Pattern EMOJI_ONLY = Pattern.compile("^[\\p{So}\\p{Sk}\\p{Cn}\\p{M}\\s\\u200D\\uFE0F]+$");
    private static final Pattern DESIGN_OBJECT = Pattern.compile(
            "(?i).*(设计|海报|logo|标志|画册|宣传册|包装|名片|菜单|展板|易拉宝|横幅|banner|封面|详情页|主图|广告图|视觉|品牌|字体|插画|物料).*"
    );
    private static final Pattern ACTIONABLE_DETAIL = Pattern.compile(
            "(?i).*(制作|改版|排版|用于|用途|投放|宣传|门店|尺寸|风格|文案).*"
    );

    @Override
    public Decision evaluate(String content, boolean hasAttachments) {
        String normalized = content == null ? "" : content.strip().replaceAll("[\\p{P}\\p{Z}]+", "").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return new Decision(false, hasAttachments
                    ? "请说明附件将用于什么设计，以及希望我们完成的内容。"
                    : "请告诉我们需要设计什么、使用场景或希望达到的效果。");
        }
        if (GREETINGS.contains(normalized) || EMOJI_ONLY.matcher(normalized).matches()) {
            return new Decision(false, "请继续说明需要设计的对象、用途或具体要求。");
        }
        if (DESIGN_OBJECT.matcher(normalized).matches()
                || (normalized.length() >= 6 && ACTIONABLE_DETAIL.matcher(normalized).matches())) {
            return new Decision(true, null);
        }
        return new Decision(false, "请补充要设计的对象、用途、尺寸、风格或需要解决的问题。");
    }
}
