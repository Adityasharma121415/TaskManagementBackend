

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.repository.LoanDurationRepository;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationDaoImpl implements ApplicationDao{

    private final TaskExecutionLogRepository repository;

    private final LoanDurationRepository durationRepository;

    public List<TaskExecutionLogEntity> findByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Override
    public List<TaskExecutionLogEntity> findTasksByApplicationIdSortedByUpdatedAt(String applicationId) {
        return repository.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
    }

    @Override
    public Map<String, Object> findTasksAndLoanDurationByApplicationId(String applicationId) {
        Map<String, Object> result = new HashMap<>();

        List<TaskExecutionLogEntity> tasks = repository.findByApplicationId(applicationId);
        result.put("tasks", tasks);

        Optional<LoanDurationEntity> loanDuration = durationRepository.findByApplicationId(applicationId);
        result.put("loanDurationEntity", loanDuration.orElse(null));

        return result;

    }
}