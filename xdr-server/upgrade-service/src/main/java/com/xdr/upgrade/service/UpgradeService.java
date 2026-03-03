package com.xdr.upgrade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdr.upgrade.mapper.UpgradePackageMapper;
import com.xdr.upgrade.mapper.UpgradeTaskMapper;
import com.xdr.upgrade.model.UpgradePackage;
import com.xdr.upgrade.model.UpgradeTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpgradeService {

    private final UpgradePackageMapper packageMapper;
    private final UpgradeTaskMapper taskMapper;

    public List<UpgradePackage> listPackages() {
        return packageMapper.selectList(null);
    }

    public void savePackage(UpgradePackage pkg) {
        if (pkg.getId() == null) {
            packageMapper.insert(pkg);
        } else {
            packageMapper.updateById(pkg);
        }
    }

    public UpgradeTask getPendingUpgrade(String agentId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<UpgradeTask>()
                .eq(UpgradeTask::getAgentId, agentId)
                .in(UpgradeTask::getStatus, "PENDING", "DOWNLOADING", "INSTALLING")
                .last("LIMIT 1"));
    }

    public void updateTaskStatus(String agentId, String status, String error, Integer progress) {
        UpgradeTask task = taskMapper.selectOne(new LambdaQueryWrapper<UpgradeTask>()
                .eq(UpgradeTask::getAgentId, agentId)
                .in(UpgradeTask::getStatus, "PENDING", "DOWNLOADING", "INSTALLING")
                .last("LIMIT 1"));
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(error);
            task.setProgress(progress);
            taskMapper.updateById(task);
        }
    }
}
