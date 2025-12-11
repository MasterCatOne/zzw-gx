-- 工序字典增加类别
use zzw_gx;

ALTER TABLE process_catalog
    ADD COLUMN category VARCHAR(20) COMMENT '工序类别：EXCAVATION/HAULING/SUPPORT/SHOTCRETE' AFTER process_code;

-- 工序表增加类别
ALTER TABLE process
    ADD COLUMN category VARCHAR(20) COMMENT '工序类别：EXCAVATION/HAULING/SUPPORT/SHOTCRETE' AFTER process_catalog_id;

-- 按现有名称回填（示例，可按你表格实际名称调整）
UPDATE process_catalog SET category='EXCAVATION' WHERE process_name IN ('挖机扒渣','测量放样','炮眼钻孔','装药爆破');
UPDATE process_catalog SET category='HAULING'   WHERE process_name IN ('通风','出渣');
UPDATE process_catalog SET category='SUPPORT'   WHERE process_name IN ('排险、扒渣、断面扫描','初喷','立架','锁脚、锚杆、超前');
UPDATE process_catalog SET category='SHOTCRETE' WHERE process_name IN ('报检','喷混');
-- 同步已有工序
UPDATE process p
    JOIN process_catalog c ON p.process_catalog_id = c.id
    SET p.category = c.category
WHERE p.category IS NULL;