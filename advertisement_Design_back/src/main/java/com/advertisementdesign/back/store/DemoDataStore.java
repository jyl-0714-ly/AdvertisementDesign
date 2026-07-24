package com.advertisementdesign.back.store;

import com.advertisementdesign.back.domain.entity.*;
import com.advertisementdesign.back.domain.enums.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class DemoDataStore {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AtomicLong userSeq = new AtomicLong(2);
    private final AtomicLong fileSeq = new AtomicLong(2);
    private final AtomicLong portfolioSeq = new AtomicLong(6);
    private final AtomicLong projectSeq = new AtomicLong(2);
    private final AtomicLong conversationSeq = new AtomicLong(2);
    private final AtomicLong messageSeq = new AtomicLong(12);
    private final AtomicLong stageSeq = new AtomicLong(0);
    private final AtomicLong stageActionSeq = new AtomicLong(7);
    private final AtomicLong projectFileSeq = new AtomicLong(2);
    private final AtomicLong readStateSeq = new AtomicLong(4);
    private final AtomicLong operationLogSeq = new AtomicLong(4);
    private final AtomicLong consultantIntakeSeq = new AtomicLong(0);
    private final AtomicLong consultantHumanMessageSeq = new AtomicLong(0);

    private final Map<Long, UserEntity> users = new LinkedHashMap<>();
    private final Map<Long, FileAssetEntity> fileAssets = new LinkedHashMap<>();
    private final Map<Long, PortfolioCaseEntity> portfolioCases = new LinkedHashMap<>();
    private final Map<Long, ProjectEntity> projects = new LinkedHashMap<>();
    private final Map<Long, ConversationEntity> conversations = new LinkedHashMap<>();
    private final Map<Long, MessageEntity> messages = new LinkedHashMap<>();
    private final Map<Long, ProjectStageEntity> stages = new LinkedHashMap<>();
    private final Map<Long, StageActionEntity> stageActions = new LinkedHashMap<>();
    private final Map<Long, ProjectFileEntity> projectFiles = new LinkedHashMap<>();
    private final Map<Long, ConversationReadStateEntity> readStates = new LinkedHashMap<>();
    private final Map<Long, OperationLogEntity> operationLogs = new LinkedHashMap<>();
    private final Map<Long, ConsultantIntakeEntity> consultantIntakes = new LinkedHashMap<>();
    private final Map<Long, ConsultantHumanMessageEntity> consultantHumanMessages = new LinkedHashMap<>();
    private final Map<Long, DesignerProfileEntity> designerProfiles = new LinkedHashMap<>();

    public DemoDataStore() {
        seed();
    }

    private void seed() {
        UserEntity customer = UserEntity.builder()
                .id(1L)
                .email("customer@163.com")
                .phone(null)
                .passwordHash("$2a$10$jFVkPRlTCuJNU3/bc97SZO4GjjiK9QRRIk8pH82/AUt5Efxlxttte")
                .nickname("演示客户")
                .role(UserRole.CUSTOMER)
                .avatar("https://example.com/avatar/customer.png")
                .status(UserStatus.ENABLED)
                .createdAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build();
        UserEntity designer = UserEntity.builder()
                .id(2L)
                .email("designer@example.com")
                .phone(null)
                .passwordHash("$2a$10$ycsJGmPT5IGSN1bN5vTwA.J.8v83fnmr2RtMDGk3OLbPvjrc5en6S")
                .nickname("演示设计师")
                .role(UserRole.DESIGNER)
                .avatar("https://example.com/avatar/designer.png")
                .status(UserStatus.ENABLED)
                .createdAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build();
        users.put(customer.getId(), customer);
        users.put(designer.getId(), designer);
        designerProfiles.put(designer.getId(), DesignerProfileEntity.builder()
                .designerId(designer.getId())
                .enabled(true)
                .online(true)
                .specialties(List.of("品牌设计", "海报设计", "餐饮", "教育"))
                .build());

        FileAssetEntity reportFile = FileAssetEntity.builder()
                .id(1L)
                .uploaderId(2L)
                .originalName("山野咖啡资料调研报告.pdf")
                .storageName("project-1-research-report.pdf")
                .storageProvider(StorageProvider.LOCAL)
                .bucketName(null)
                .objectKey("demo/project-1/project-1-research-report.pdf")
                .url("https://example.com/files/project-1-research-report.pdf")
                .mimeType("application/pdf")
                .fileSize(2_483_200L)
                .fileHash("demo-hash-project-1-report")
                .status(FileStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 7, 20, 11, 20))
                .updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20))
                .build();
        FileAssetEntity sketchFile = FileAssetEntity.builder()
                .id(2L)
                .uploaderId(2L)
                .originalName("启星教育草图方向稿.zip")
                .storageName("project-2-sketch-draft.zip")
                .storageProvider(StorageProvider.LOCAL)
                .bucketName(null)
                .objectKey("demo/project-2/project-2-sketch-draft.zip")
                .url("https://example.com/files/project-2-sketch-draft.zip")
                .mimeType("application/zip")
                .fileSize(5_242_880L)
                .fileHash("demo-hash-project-2-draft")
                .status(FileStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 7, 20, 11, 30))
                .updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30))
                .build();
        fileAssets.put(reportFile.getId(), reportFile);
        fileAssets.put(sketchFile.getId(), sketchFile);

        portfolioCases.put(1L, PortfolioCaseEntity.builder().id(1L).title("山野咖啡品牌视觉升级").category(PortfolioCategory.BRAND).industry("餐饮").style("极简").serviceType("品牌设计").coverUrl("https://example.com/portfolio/cafe-cover.jpg").imageUrls(List.of("https://example.com/portfolio/cafe-1.jpg", "https://example.com/portfolio/cafe-2.jpg")).description("为精品咖啡品牌重构 Logo、主视觉和门店物料，突出自然、手作和社区感。").sortOrder(1).featured(true).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 5)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 5)).build());
        portfolioCases.put(2L, PortfolioCaseEntity.builder().id(2L).title("启星少儿教育招生海报").category(PortfolioCategory.DIGITAL).industry("教育").style("年轻化").serviceType("海报设计").coverUrl("https://example.com/portfolio/education-cover.jpg").imageUrls(List.of("https://example.com/portfolio/education-1.jpg", "https://example.com/portfolio/education-2.jpg")).description("围绕暑期招生场景设计线上线下海报，强化课程亮点和行动入口。").sortOrder(2).featured(false).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 6)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 6)).build());
        portfolioCases.put(3L, PortfolioCaseEntity.builder().id(3L).title("云栖地产高端画册").category(PortfolioCategory.OFFLINE).industry("地产").style("高端").serviceType("画册设计").coverUrl("https://example.com/portfolio/estate-cover.jpg").imageUrls(List.of("https://example.com/portfolio/estate-1.jpg", "https://example.com/portfolio/estate-2.jpg")).description("为高端住宅项目设计招商画册，强调空间质感、区位价值和生活方式。").sortOrder(3).featured(true).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 7)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 7)).build());
        portfolioCases.put(4L, PortfolioCaseEntity.builder().id(4L).title("潮玩电商活动视觉").category(PortfolioCategory.DIGITAL).industry("电商").style("国潮").serviceType("活动物料").coverUrl("https://example.com/portfolio/ecommerce-cover.jpg").imageUrls(List.of("https://example.com/portfolio/ecommerce-1.jpg", "https://example.com/portfolio/ecommerce-2.jpg")).description("为电商大促设计主 KV、商品卡片和社媒传播图，提升点击和转化。").sortOrder(4).featured(true).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 8)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 8)).build());
        portfolioCases.put(5L, PortfolioCaseEntity.builder().id(5L).title("智造科技企业 VI 系统").category(PortfolioCategory.BRAND).industry("科技").style("商务").serviceType("VI 设计").coverUrl("https://example.com/portfolio/tech-cover.jpg").imageUrls(List.of("https://example.com/portfolio/tech-1.jpg", "https://example.com/portfolio/tech-2.jpg")).description("为工业科技企业建立统一 VI 系统，覆盖名片、PPT、展板和官网视觉规范。").sortOrder(5).featured(false).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 9)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 9)).build());
        portfolioCases.put(6L, PortfolioCaseEntity.builder().id(6L).title("新锐美妆包装设计").category(PortfolioCategory.OFFLINE).industry("美妆").style("年轻化").serviceType("包装设计").coverUrl("https://example.com/portfolio/beauty-cover.jpg").imageUrls(List.of("https://example.com/portfolio/beauty-1.jpg", "https://example.com/portfolio/beauty-2.jpg")).description("围绕年轻女性消费场景打造包装视觉，突出轻盈、清洁和系列化陈列效果。").sortOrder(6).featured(false).status(PortfolioStatus.PUBLISHED).createdAt(LocalDateTime.of(2026, 7, 20, 10, 10)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 10)).build());

        ProjectEntity project1 = ProjectEntity.builder().id(1L).name("山野咖啡品牌升级项目").customerId(1L).designerId(2L).description("精品咖啡品牌视觉升级，覆盖 Logo、门店物料和线上传播图。").currentStage("RESEARCH_REPORT").status(ProjectStatus.IN_PROGRESS).progress(28).createdAt(LocalDateTime.of(2026, 7, 20, 10, 20)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build();
        ProjectEntity project2 = ProjectEntity.builder().id(2L).name("启星教育暑期招生海报项目").customerId(1L).designerId(2L).description("暑期招生海报和活动视觉设计，突出课程卖点和报名转化。").currentStage("SKETCH_STYLE").status(ProjectStatus.IN_PROGRESS).progress(43).createdAt(LocalDateTime.of(2026, 7, 20, 10, 30)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build();
        projects.put(1L, project1);
        projects.put(2L, project2);

        conversations.put(1L, ConversationEntity.builder().id(1L).projectId(1L).customerId(1L).designerId(2L).lastMessage("资料调研报告已提交，请客户确认。").lastMessageAt(LocalDateTime.of(2026, 7, 20, 11, 20)).createdAt(LocalDateTime.of(2026, 7, 20, 10, 20)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        conversations.put(2L, ConversationEntity.builder().id(2L).projectId(2L).customerId(1L).designerId(2L).lastMessage("草图方向需要再年轻化一点。").lastMessageAt(LocalDateTime.of(2026, 7, 20, 11, 30)).createdAt(LocalDateTime.of(2026, 7, 20, 10, 30)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build());

        messages.put(1L, MessageEntity.builder().id(1L).conversationId(1L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("项目已创建，双方可以开始需求沟通。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 20)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 20)).fileIds(List.of()).build());
        messages.put(2L, MessageEntity.builder().id(2L).conversationId(1L).senderId(1L).senderRole(MessageSenderRole.CUSTOMER).messageType(MessageType.TEXT).content("我们希望咖啡品牌整体更自然，适合社区门店和线上传播。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 25)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 25)).fileIds(List.of()).build());
        messages.put(3L, MessageEntity.builder().id(3L).conversationId(1L).senderId(2L).senderRole(MessageSenderRole.DESIGNER).messageType(MessageType.TEXT).content("收到，我会先整理需求模板，并补充品牌调研方向。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 28)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 28)).fileIds(List.of()).build());
        messages.put(4L, MessageEntity.builder().id(4L).conversationId(1L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("阶段「需求引导」已达成。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 40)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 40)).fileIds(List.of()).build());
        messages.put(5L, MessageEntity.builder().id(5L).conversationId(1L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("阶段「签订合同预付款」已达成。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 11, 0)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).fileIds(List.of()).build());
        messages.put(6L, MessageEntity.builder().id(6L).conversationId(1L).senderId(2L).senderRole(MessageSenderRole.DESIGNER).messageType(MessageType.TEXT).content("资料调研报告已提交，请客户确认。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 11, 20)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).fileIds(List.of(1L)).build());
        messages.put(7L, MessageEntity.builder().id(7L).conversationId(2L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("项目已创建，双方可以开始需求沟通。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 30)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 30)).fileIds(List.of()).build());
        messages.put(8L, MessageEntity.builder().id(8L).conversationId(2L).senderId(1L).senderRole(MessageSenderRole.CUSTOMER).messageType(MessageType.TEXT).content("这次招生海报需要更活泼，突出暑期课程优惠。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 35)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 35)).fileIds(List.of()).build());
        messages.put(9L, MessageEntity.builder().id(9L).conversationId(2L).senderId(2L).senderRole(MessageSenderRole.DESIGNER).messageType(MessageType.TEXT).content("我会提供两个风格方向，一个偏清爽，一个偏高饱和。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 10, 50)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 50)).fileIds(List.of()).build());
        messages.put(10L, MessageEntity.builder().id(10L).conversationId(2L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("阶段「需求引导」已达成。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 11, 0)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).fileIds(List.of()).build());
        messages.put(11L, MessageEntity.builder().id(11L).conversationId(2L).senderId(null).senderRole(MessageSenderRole.SYSTEM).messageType(MessageType.SYSTEM).content("阶段「资料调研报告」已驳回。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 11, 15)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 15)).fileIds(List.of()).build());
        messages.put(12L, MessageEntity.builder().id(12L).conversationId(2L).senderId(1L).senderRole(MessageSenderRole.CUSTOMER).messageType(MessageType.TEXT).content("草图方向需要再年轻化一点。").replyToMessageId(null).clientMessageId(null).isDeleted(false).createdAt(LocalDateTime.of(2026, 7, 20, 11, 30)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).fileIds(List.of(2L)).build());

        stagePut(1L, "REQUIREMENT_GUIDE", "需求引导", 1, ProjectStageStatus.REACHED, LocalDateTime.of(2026, 7, 20, 10, 40));
        stagePut(1L, "CONTRACT_PREPAYMENT", "签订合同预付款", 2, ProjectStageStatus.REACHED, LocalDateTime.of(2026, 7, 20, 11, 0));
        stagePut(1L, "RESEARCH_REPORT", "资料调研报告", 3, ProjectStageStatus.PENDING_CONFIRM, null);
        stagePut(1L, "SKETCH_STYLE", "草图风格敲定", 4, ProjectStageStatus.TODO, null);
        stagePut(1L, "REVIEW_FINAL", "审稿定稿", 5, ProjectStageStatus.TODO, null);
        stagePut(1L, "FINAL_PAYMENT", "交付尾款", 6, ProjectStageStatus.TODO, null);
        stagePut(1L, "AFTER_SALE_REPURCHASE", "售后复购", 7, ProjectStageStatus.TODO, null);

        stagePut(2L, "REQUIREMENT_GUIDE", "需求引导", 1, ProjectStageStatus.REACHED, LocalDateTime.of(2026, 7, 20, 11, 0));
        stagePut(2L, "CONTRACT_PREPAYMENT", "签订合同预付款", 2, ProjectStageStatus.REACHED, LocalDateTime.of(2026, 7, 20, 11, 5));
        stagePut(2L, "RESEARCH_REPORT", "资料调研报告", 3, ProjectStageStatus.REJECTED, null);
        stagePut(2L, "SKETCH_STYLE", "草图风格敲定", 4, ProjectStageStatus.PENDING_CONFIRM, null);
        stagePut(2L, "REVIEW_FINAL", "审稿定稿", 5, ProjectStageStatus.TODO, null);
        stagePut(2L, "FINAL_PAYMENT", "交付尾款", 6, ProjectStageStatus.TODO, null);
        stagePut(2L, "AFTER_SALE_REPURCHASE", "售后复购", 7, ProjectStageStatus.TODO, null);

        stageActions.put(1L, StageActionEntity.builder().id(1L).projectId(1L).projectStageId(1L).stageCode("REQUIREMENT_GUIDE").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.CONFIRMED).requestNote("已发送需求引导模板，请确认。").responseNote("确认需求方向。").requestedAt(LocalDateTime.of(2026, 7, 20, 10, 35)).respondedAt(LocalDateTime.of(2026, 7, 20, 10, 40)).createdAt(LocalDateTime.of(2026, 7, 20, 10, 35)).updatedAt(LocalDateTime.of(2026, 7, 20, 10, 40)).build());
        stageActions.put(2L, StageActionEntity.builder().id(2L).projectId(1L).projectStageId(2L).stageCode("CONTRACT_PREPAYMENT").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.CONFIRMED).requestNote("合同和预付款节点已准备。").responseNote("确认进入调研阶段。").requestedAt(LocalDateTime.of(2026, 7, 20, 10, 55)).respondedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).createdAt(LocalDateTime.of(2026, 7, 20, 10, 55)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).build());
        stageActions.put(3L, StageActionEntity.builder().id(3L).projectId(1L).projectStageId(3L).stageCode("RESEARCH_REPORT").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.PENDING).requestNote("资料调研报告已提交，请客户确认。").responseNote(null).requestedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).respondedAt(null).createdAt(LocalDateTime.of(2026, 7, 20, 11, 20)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        stageActions.put(4L, StageActionEntity.builder().id(4L).projectId(2L).projectStageId(8L).stageCode("REQUIREMENT_GUIDE").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.CONFIRMED).requestNote("已发送招生海报需求引导。").responseNote("确认。").requestedAt(LocalDateTime.of(2026, 7, 20, 10, 55)).respondedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).createdAt(LocalDateTime.of(2026, 7, 20, 10, 55)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 0)).build());
        stageActions.put(5L, StageActionEntity.builder().id(5L).projectId(2L).projectStageId(9L).stageCode("CONTRACT_PREPAYMENT").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.CONFIRMED).requestNote("合同预付款节点确认。").responseNote("确认。").requestedAt(LocalDateTime.of(2026, 7, 20, 11, 2)).respondedAt(LocalDateTime.of(2026, 7, 20, 11, 5)).createdAt(LocalDateTime.of(2026, 7, 20, 11, 2)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 5)).build());
        stageActions.put(6L, StageActionEntity.builder().id(6L).projectId(2L).projectStageId(10L).stageCode("RESEARCH_REPORT").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.REJECTED).requestNote("资料调研报告已提交。").responseNote("需要补充竞品参考。").requestedAt(LocalDateTime.of(2026, 7, 20, 11, 10)).respondedAt(LocalDateTime.of(2026, 7, 20, 11, 15)).createdAt(LocalDateTime.of(2026, 7, 20, 11, 10)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 15)).build());
        stageActions.put(7L, StageActionEntity.builder().id(7L).projectId(2L).projectStageId(11L).stageCode("SKETCH_STYLE").initiatorId(2L).initiatorRole(MessageSenderRole.DESIGNER).confirmUserId(1L).status(StageActionStatus.PENDING).requestNote("草图方向稿已提交，请确认。").responseNote(null).requestedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).respondedAt(null).createdAt(LocalDateTime.of(2026, 7, 20, 11, 30)).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build());

        projectFiles.put(1L, ProjectFileEntity.builder().id(1L).projectId(1L).projectStageId(3L).stageCode("RESEARCH_REPORT").fileId(1L).uploaderId(2L).fileRole(FileRole.REPORT).description("山野咖啡资料调研报告演示文件。").createdAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        projectFiles.put(2L, ProjectFileEntity.builder().id(2L).projectId(2L).projectStageId(11L).stageCode("SKETCH_STYLE").fileId(2L).uploaderId(2L).fileRole(FileRole.DRAFT).description("启星教育草图方向稿演示文件。").createdAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build());

        readStates.put(1L, ConversationReadStateEntity.builder().id(1L).conversationId(1L).userId(1L).lastReadMessageId(5L).lastReadAt(LocalDateTime.of(2026, 7, 20, 11, 5)).unreadCount(1).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        readStates.put(2L, ConversationReadStateEntity.builder().id(2L).conversationId(1L).userId(2L).lastReadMessageId(6L).lastReadAt(LocalDateTime.of(2026, 7, 20, 11, 20)).unreadCount(0).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        readStates.put(3L, ConversationReadStateEntity.builder().id(3L).conversationId(2L).userId(1L).lastReadMessageId(12L).lastReadAt(LocalDateTime.of(2026, 7, 20, 11, 30)).unreadCount(0).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build());
        readStates.put(4L, ConversationReadStateEntity.builder().id(4L).conversationId(2L).userId(2L).lastReadMessageId(11L).lastReadAt(LocalDateTime.of(2026, 7, 20, 11, 20)).unreadCount(1).updatedAt(LocalDateTime.of(2026, 7, 20, 11, 30)).build());

        operationLogs.put(1L, OperationLogEntity.builder().id(1L).operatorId(null).operatorRole(MessageSenderRole.SYSTEM).bizType("PROJECT").bizId(1L).action("CREATE").description("系统初始化山野咖啡品牌升级项目。").beforeData(null).afterData(Map.of("status", "IN_PROGRESS")).createdAt(LocalDateTime.of(2026, 7, 20, 10, 20)).build());
        operationLogs.put(2L, OperationLogEntity.builder().id(2L).operatorId(null).operatorRole(MessageSenderRole.SYSTEM).bizType("PROJECT").bizId(2L).action("CREATE").description("系统初始化启星教育暑期招生海报项目。").beforeData(null).afterData(Map.of("status", "IN_PROGRESS")).createdAt(LocalDateTime.of(2026, 7, 20, 10, 30)).build());
        operationLogs.put(3L, OperationLogEntity.builder().id(3L).operatorId(2L).operatorRole(MessageSenderRole.DESIGNER).bizType("STAGE").bizId(1L).action("REQUEST_CONFIRM").description("设计师发起资料调研报告确认。").beforeData(Map.of("status", "TODO")).afterData(Map.of("status", "PENDING_CONFIRM")).createdAt(LocalDateTime.of(2026, 7, 20, 11, 20)).build());
        operationLogs.put(4L, OperationLogEntity.builder().id(4L).operatorId(1L).operatorRole(MessageSenderRole.CUSTOMER).bizType("STAGE").bizId(2L).action("REJECT").description("客户驳回资料调研报告。").beforeData(Map.of("status", "PENDING_CONFIRM")).afterData(Map.of("status", "REJECTED")).createdAt(LocalDateTime.of(2026, 7, 20, 11, 15)).build());
    }

    private void stagePut(Long projectId, String stageCode, String stageName, int sortOrder, ProjectStageStatus status, LocalDateTime reachedAt) {
        long id = stageSeq.incrementAndGet();
        stages.put(id, ProjectStageEntity.builder()
                .id(id)
                .projectId(projectId)
                .stageCode(stageCode)
                .stageName(stageName)
                .sortOrder(sortOrder)
                .status(status)
                .reachedAt(reachedAt)
                .updatedAt(reachedAt == null ? LocalDateTime.of(2026, 7, 20, 11, 20) : reachedAt)
                .build());
    }

    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    public synchronized Optional<UserEntity> findUserByEmail(String email) {
        return users.values().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    public synchronized Optional<UserEntity> findUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public synchronized UserEntity saveUser(UserEntity user) {
        if (user.getId() == null) {
            user.setId(userSeq.incrementAndGet());
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        users.put(user.getId(), user);
        return user;
    }

    public synchronized List<PortfolioCaseEntity> listPortfolioCases(PortfolioCategory category, String industry, String style, String keyword, Boolean featured) {
        return portfolioCases.values().stream()
                .filter(item -> category == null || category == item.getCategory())
                .filter(item -> industry == null || industry.isBlank() || industry.equalsIgnoreCase(item.getIndustry()))
                .filter(item -> style == null || style.isBlank() || style.equalsIgnoreCase(item.getStyle()))
                .filter(item -> featured == null || featured.equals(Boolean.TRUE.equals(item.getFeatured())))
                .filter(item -> keyword == null || keyword.isBlank() || containsKeyword(item, keyword))
                .sorted(Comparator.comparingInt(PortfolioCaseEntity::getSortOrder))
                .collect(Collectors.toList());
    }

    private boolean containsKeyword(PortfolioCaseEntity item, String keyword) {
        String lower = keyword.toLowerCase();
        return item.getTitle().toLowerCase().contains(lower)
                || item.getDescription().toLowerCase().contains(lower)
                || item.getIndustry().toLowerCase().contains(lower)
                || item.getStyle().toLowerCase().contains(lower)
                || item.getServiceType().toLowerCase().contains(lower);
    }

    public synchronized Optional<PortfolioCaseEntity> findPortfolioCaseById(Long id) {
        return Optional.ofNullable(portfolioCases.get(id));
    }

    public synchronized PortfolioCaseEntity savePortfolioCase(PortfolioCaseEntity portfolioCase) {
        if (portfolioCase.getId() == null) {
            portfolioCase.setId(portfolioSeq.incrementAndGet());
            portfolioCase.setCreatedAt(LocalDateTime.now());
        }
        portfolioCase.setUpdatedAt(LocalDateTime.now());
        portfolioCases.put(portfolioCase.getId(), portfolioCase);
        return portfolioCase;
    }

    public synchronized List<ProjectEntity> listProjects() {
        return new ArrayList<>(projects.values());
    }

    public synchronized Optional<ProjectEntity> findProjectById(Long id) {
        return Optional.ofNullable(projects.get(id));
    }

    public synchronized ProjectEntity saveProject(ProjectEntity project) {
        if (project.getId() == null) {
            project.setId(projectSeq.incrementAndGet());
            project.setCreatedAt(LocalDateTime.now());
        }
        project.setUpdatedAt(LocalDateTime.now());
        projects.put(project.getId(), project);
        return project;
    }

    public synchronized List<ConversationEntity> listConversations() {
        return new ArrayList<>(conversations.values());
    }

    public synchronized Optional<ConversationEntity> findConversationById(Long id) {
        return Optional.ofNullable(conversations.get(id));
    }

    public synchronized Optional<ConversationEntity> findConversationByProjectId(Long projectId) {
        return conversations.values().stream().filter(item -> item.getProjectId().equals(projectId)).findFirst();
    }

    public synchronized ConversationEntity saveConversation(ConversationEntity conversation) {
        if (conversation.getId() == null) {
            conversation.setId(conversationSeq.incrementAndGet());
            conversation.setCreatedAt(LocalDateTime.now());
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversations.put(conversation.getId(), conversation);
        return conversation;
    }

    public synchronized List<MessageEntity> listMessages(Long conversationId) {
        return messages.values().stream()
                .filter(message -> message.getConversationId().equals(conversationId))
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt))
                .collect(Collectors.toList());
    }

    public synchronized Optional<MessageEntity> findMessageById(Long id) {
        return Optional.ofNullable(messages.get(id));
    }

    public synchronized MessageEntity saveMessage(MessageEntity message) {
        if (message.getId() == null) {
            message.setId(messageSeq.incrementAndGet());
            message.setCreatedAt(LocalDateTime.now());
        }
        message.setUpdatedAt(LocalDateTime.now());
        messages.put(message.getId(), message);
        return message;
    }

    public synchronized List<ProjectStageEntity> listStages(Long projectId) {
        return stages.values().stream()
                .filter(stage -> stage.getProjectId().equals(projectId))
                .sorted(Comparator.comparing(ProjectStageEntity::getSortOrder))
                .collect(Collectors.toList());
    }

    public synchronized List<ProjectStageEntity> findStages(Long projectId) {
        return listStages(projectId);
    }

    public synchronized Optional<ProjectStageEntity> findStage(Long projectId, String stageCode) {
        return stages.values().stream()
                .filter(stage -> stage.getProjectId().equals(projectId) && stage.getStageCode().equals(stageCode))
                .findFirst();
    }

    public synchronized Optional<ProjectStageEntity> findStageById(Long id) {
        return Optional.ofNullable(stages.get(id));
    }

    public synchronized ProjectStageEntity saveStage(ProjectStageEntity stage) {
        if (stage.getId() == null) {
            stage.setId(stageSeq.incrementAndGet());
        }
        stage.setUpdatedAt(LocalDateTime.now());
        stages.put(stage.getId(), stage);
        return stage;
    }

    public synchronized StageActionEntity saveStageAction(StageActionEntity stageAction) {
        if (stageAction.getId() == null) {
            stageAction.setId(stageActionSeq.incrementAndGet());
            stageAction.setCreatedAt(LocalDateTime.now());
        }
        stageAction.setUpdatedAt(LocalDateTime.now());
        stageActions.put(stageAction.getId(), stageAction);
        return stageAction;
    }

    public synchronized Optional<StageActionEntity> findStageActionById(Long id) {
        return Optional.ofNullable(stageActions.get(id));
    }

    public synchronized List<StageActionEntity> listStageActions(Long projectId, String stageCode, StageActionStatus status) {
        return stageActions.values().stream()
                .filter(action -> action.getProjectId().equals(projectId))
                .filter(action -> stageCode == null || stageCode.isBlank() || stageCode.equals(action.getStageCode()))
                .filter(action -> status == null || status.equals(action.getStatus()))
                .sorted(Comparator.comparing(StageActionEntity::getRequestedAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized FileAssetEntity saveFileAsset(FileAssetEntity fileAsset) {
        if (fileAsset.getId() == null) {
            fileAsset.setId(fileSeq.incrementAndGet());
            fileAsset.setCreatedAt(LocalDateTime.now());
        }
        fileAsset.setUpdatedAt(LocalDateTime.now());
        fileAssets.put(fileAsset.getId(), fileAsset);
        return fileAsset;
    }

    public synchronized Optional<FileAssetEntity> findFileAssetById(Long id) {
        return Optional.ofNullable(fileAssets.get(id));
    }

    public synchronized List<ProjectFileEntity> listProjectFiles(Long projectId, String stageCode, FileRole fileRole) {
        return projectFiles.values().stream()
                .filter(file -> file.getProjectId().equals(projectId))
                .filter(file -> stageCode == null || stageCode.isBlank() || stageCode.equals(file.getStageCode()))
                .filter(file -> fileRole == null || fileRole.equals(file.getFileRole()))
                .sorted(Comparator.comparing(ProjectFileEntity::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized ProjectFileEntity saveProjectFile(ProjectFileEntity projectFile) {
        if (projectFile.getId() == null) {
            projectFile.setId(projectFileSeq.incrementAndGet());
            projectFile.setCreatedAt(LocalDateTime.now());
        }
        projectFiles.put(projectFile.getId(), projectFile);
        return projectFile;
    }

    public synchronized Optional<ProjectFileEntity> findProjectFileById(Long id) {
        return Optional.ofNullable(projectFiles.get(id));
    }

    public synchronized boolean deleteProjectFile(Long id) {
        return projectFiles.remove(id) != null;
    }

    public synchronized ConversationReadStateEntity saveReadState(ConversationReadStateEntity readState) {
        if (readState.getId() == null) {
            readState.setId(readStateSeq.incrementAndGet());
        }
        readState.setUpdatedAt(LocalDateTime.now());
        readStates.put(readState.getId(), readState);
        return readState;
    }

    public synchronized Optional<ConversationReadStateEntity> findReadState(Long conversationId, Long userId) {
        return readStates.values().stream()
                .filter(state -> state.getConversationId().equals(conversationId) && state.getUserId().equals(userId))
                .findFirst();
    }

    public synchronized List<ConversationReadStateEntity> listReadStates(Long conversationId) {
        return readStates.values().stream()
                .filter(state -> state.getConversationId().equals(conversationId))
                .collect(Collectors.toList());
    }

    public synchronized List<OperationLogEntity> listOperationLogs(Long projectId) {
        return operationLogs.values().stream()
                .filter(log -> projectId.equals(log.getBizId()))
                .sorted(Comparator.comparing(OperationLogEntity::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized OperationLogEntity saveOperationLog(OperationLogEntity log) {
        if (log.getId() == null) {
            log.setId(operationLogSeq.incrementAndGet());
            log.setCreatedAt(LocalDateTime.now());
        }
        operationLogs.put(log.getId(), log);
        return log;
    }

    public synchronized List<DesignerProfileEntity> listDesignerProfiles() {
        return new ArrayList<>(designerProfiles.values());
    }

    public synchronized Optional<DesignerProfileEntity> findDesignerProfile(Long designerId) {
        return Optional.ofNullable(designerProfiles.get(designerId));
    }

    public synchronized DesignerProfileEntity saveDesignerProfile(DesignerProfileEntity profile) {
        designerProfiles.put(profile.getDesignerId(), profile);
        return profile;
    }

    public synchronized ConsultantIntakeEntity saveConsultantIntake(ConsultantIntakeEntity intake) {
        if (intake.getId() == null) {
            intake.setId(consultantIntakeSeq.incrementAndGet());
            intake.setCreatedAt(LocalDateTime.now());
        }
        intake.setUpdatedAt(LocalDateTime.now());
        consultantIntakes.put(intake.getId(), intake);
        return intake;
    }

    public synchronized Optional<ConsultantIntakeEntity> findConsultantIntakeById(Long id) {
        return Optional.ofNullable(consultantIntakes.get(id));
    }

    public synchronized Optional<ConsultantIntakeEntity> findConsultantIntakeByHumanChatId(String humanChatId) {
        return consultantIntakes.values().stream()
                .filter(intake -> Objects.equals(humanChatId, intake.getHumanChatId()))
                .findFirst();
    }

    public synchronized ConsultantHumanMessageEntity saveConsultantHumanMessage(ConsultantHumanMessageEntity message) {
        if (message.getId() == null) {
            message.setId(consultantHumanMessageSeq.incrementAndGet());
            message.setCreatedAt(LocalDateTime.now());
        }
        consultantHumanMessages.put(message.getId(), message);
        return message;
    }

    public synchronized List<ConsultantHumanMessageEntity> listConsultantHumanMessages(String humanChatId) {
        return consultantHumanMessages.values().stream()
                .filter(message -> Objects.equals(humanChatId, message.getHumanChatId()))
                .sorted(Comparator.comparing(ConsultantHumanMessageEntity::getCreatedAt)
                        .thenComparing(ConsultantHumanMessageEntity::getId))
                .collect(Collectors.toList());
    }

    public synchronized long countInProgressProjectsByDesigner(Long designerId) {
        return projects.values().stream()
                .filter(project -> designerId.equals(project.getDesignerId()))
                .filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();
    }

    public synchronized long countReachedStages(Long projectId) {
        return listStages(projectId).stream().filter(stage -> stage.getStatus() == ProjectStageStatus.REACHED).count();
    }

    public synchronized void refreshProjectProgress(Long projectId) {
        ProjectEntity project = projects.get(projectId);
        if (project == null) {
            return;
        }
        long reached = countReachedStages(projectId);
        int progress = (int) Math.round(reached * 100.0 / 7.0);
        project.setProgress(progress);
        project.setUpdatedAt(LocalDateTime.now());
        if (reached >= 7) {
            project.setStatus(ProjectStatus.COMPLETED);
        }
    }

    public synchronized int unreadCount(Long conversationId, Long userId) {
        return findReadState(conversationId, userId).map(ConversationReadStateEntity::getUnreadCount).orElse(0);
    }
}
