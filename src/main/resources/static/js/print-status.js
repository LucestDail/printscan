document.addEventListener('DOMContentLoaded', function() {
    const printJobsList = document.getElementById('printJobsList');
    const emptyState = document.getElementById('emptyState');
    const jobDetailModal = new bootstrap.Modal(document.getElementById('jobDetailModal'));
    const cancelJobBtn = document.getElementById('cancelJobBtn');
    let currentJobId = null;
    let currentPage = 0;
    let pageSize = 10;
    let totalPages = 0;

    // 작업 목록 로드
    async function loadPrintJobs() {
        try {
            showLoading();
            
            const status = document.getElementById('status').value;
            const priority = document.getElementById('priority').value;
            const department = document.getElementById('department').value;
            const search = document.getElementById('search').value;
            
            const response = await fetch(`/api/print/jobs?page=${currentPage}&size=${pageSize}&status=${status}&priority=${priority}&department=${department}&search=${search}`);
            
            if (!response.ok) {
                throw new Error('인쇄 작업 목록을 가져오는데 실패했습니다.');
            }
            
            const data = await response.json();
            renderPrintJobs(data.content);
            renderPagination(data.totalPages);
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    }

    // 작업 상세 정보 표시
    window.showJobDetail = async function(jobId) {
        try {
            showLoading();
            
            const response = await fetch(`/api/print/jobs/${jobId}`);
            if (!response.ok) {
                throw new Error('작업 정보를 가져오는데 실패했습니다.');
            }
            
            const job = await response.json();
            
            currentJobId = job.id;
            document.getElementById('modalTitle').textContent = job.title;
            document.getElementById('modalDepartment').textContent = job.department;
            document.getElementById('modalRequester').textContent = job.requester;
            document.getElementById('modalRequestDate').textContent = formatDate(job.requestDate);
            document.getElementById('modalStatus').innerHTML = `<span class="badge bg-${getStatusColor(job.status)}">${job.status}</span>`;
            document.getElementById('modalCopies').textContent = job.copies;
            document.getElementById('modalPriority').innerHTML = `<span class="badge bg-${getPriorityColor(job.priority)}">${job.priority}</span>`;
            document.getElementById('modalDoubleSided').textContent = job.printOptions.doubleSided ? '예' : '아니오';
            document.getElementById('modalColor').textContent = job.printOptions.color ? '예' : '아니오';
            
            // 기타 옵션
            const options = [];
            if (job.printOptions.stapled) options.push('스테이플');
            if (job.printOptions.punched) options.push('펀치');
            document.getElementById('modalOptions').textContent = options.join(', ') || '없음';
            
            // 파일 목록
            const filesList = document.getElementById('modalFiles');
            filesList.innerHTML = job.files.map(file => `
                <li class="list-group-item">
                    <i class="mdi mdi-file-document"></i> ${file.name}
                    <span class="text-muted">(${formatFileSize(file.size)})</span>
                </li>
            `).join('');
            
            // 작업 로그
            const logsList = document.getElementById('modalLogs');
            logsList.innerHTML = job.logs.map(log => `
                <li class="list-group-item">
                    <small class="text-muted">${formatDate(log.timestamp)}</small>
                    <br>${log.message}
                </li>
            `).join('');
            
            // 작업 취소 버튼 상태 설정
            cancelJobBtn.style.display = (job.status === 'WAITING' || job.status === 'PROCESSING') ? 'block' : 'none';
            cancelJobBtn.onclick = () => cancelJob(job.id);
            
            // 모달 표시
            jobDetailModal.show();
            
        } catch (error) {
            handleError(error);
        }
    };

    // 작업 취소
    window.cancelJob = async function(jobId) {
        try {
            if (!confirm('이 작업을 취소하시겠습니까?')) {
                return;
            }
            
            showLoading();
            
            const response = await fetch(`/api/print/jobs/${jobId}/cancel`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('작업 취소에 실패했습니다.');
            }
            
            showNotification('작업이 취소되었습니다.');
            loadPrintJobs();
            
            // 모달이 열려있다면 닫기
            jobDetailModal.hide();
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    };

    // 우선순위 색상
    function getPriorityColor(priority) {
        const colors = {
            'LOW': 'secondary',
            'MEDIUM': 'info',
            'HIGH': 'warning',
            'URGENT': 'danger'
        };
        return colors[priority] || 'secondary';
    }

    // 상태 색상
    function getStatusColor(status) {
        const colors = {
            'WAITING': 'secondary',
            'PROCESSING': 'primary',
            'COMPLETED': 'success',
            'CANCELED': 'danger',
            'ERROR': 'danger'
        };
        return colors[status] || 'secondary';
    }

    // 날짜 포맷
    function formatDate(dateString) {
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    // 파일 크기 포맷
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    // 인쇄 작업 목록 렌더링
    function renderPrintJobs(jobs) {
        const tbody = document.getElementById('printJobList');
        tbody.innerHTML = '';
        
        if (jobs.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center">인쇄 작업이 없습니다.</td>
                </tr>
            `;
            return;
        }
        
        jobs.forEach(job => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${job.id}</td>
                <td>${job.title}</td>
                <td>${job.department}</td>
                <td>${job.requester}</td>
                <td>${job.copies}</td>
                <td>
                    <span class="badge bg-${getPriorityColor(job.priority)}">${job.priority}</span>
                </td>
                <td>
                    <span class="badge bg-${getStatusColor(job.status)}">${job.status}</span>
                </td>
                <td>${formatDate(job.requestDate)}</td>
                <td>
                    <button class="btn btn-sm btn-info" onclick="showJobDetail(${job.id})">
                        <i class="mdi mdi-eye"></i>
                    </button>
                    ${job.status === 'WAITING' || job.status === 'PROCESSING' ? `
                        <button class="btn btn-sm btn-danger" onclick="cancelJob(${job.id})">
                            <i class="mdi mdi-cancel"></i>
                        </button>
                    ` : ''}
                </td>
            `;
            tbody.appendChild(row);
        });
    }
    
    // 페이지네이션 렌더링
    function renderPagination(totalPages) {
        const pagination = document.getElementById('pagination');
        pagination.innerHTML = '';
        
        // 이전 페이지 버튼
        const prevButton = document.createElement('li');
        prevButton.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
        prevButton.innerHTML = `
            <a class="page-link" href="#" onclick="return false;">
                <i class="mdi mdi-chevron-left"></i>
            </a>
        `;
        prevButton.addEventListener('click', () => {
            if (currentPage > 0) {
                currentPage--;
                loadPrintJobs();
            }
        });
        pagination.appendChild(prevButton);
        
        // 페이지 번호
        for (let i = 0; i < totalPages; i++) {
            const pageButton = document.createElement('li');
            pageButton.className = `page-item ${currentPage === i ? 'active' : ''}`;
            pageButton.innerHTML = `
                <a class="page-link" href="#" onclick="return false;">${i + 1}</a>
            `;
            pageButton.addEventListener('click', () => {
                currentPage = i;
                loadPrintJobs();
            });
            pagination.appendChild(pageButton);
        }
        
        // 다음 페이지 버튼
        const nextButton = document.createElement('li');
        nextButton.className = `page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
        nextButton.innerHTML = `
            <a class="page-link" href="#" onclick="return false;">
                <i class="mdi mdi-chevron-right"></i>
            </a>
        `;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadPrintJobs();
            }
        });
        pagination.appendChild(nextButton);
    }

    // 알림 표시
    function showAlert(type, message) {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
        alertDiv.style.zIndex = '1050';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(alertDiv);

        setTimeout(() => {
            alertDiv.remove();
        }, 5000);
    }

    // 초기 로드
    loadPrintJobs();

    // 주기적 새로고침 (30초마다)
    setInterval(loadPrintJobs, 30000);
}); 