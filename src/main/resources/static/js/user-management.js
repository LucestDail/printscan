document.addEventListener('DOMContentLoaded', function() {
    const filterForm = document.getElementById('filterForm');
    const userList = document.getElementById('userList');
    const pagination = document.getElementById('pagination');
    const userModal = new bootstrap.Modal(document.getElementById('userModal'));
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    let currentPage = 1;
    let totalPages = 1;
    let selectedUserId = null;

    // 초기 데이터 로드
    loadDepartments();
    loadUsers();

    // 필터 폼 제출
    filterForm.addEventListener('submit', function(e) {
        e.preventDefault();
        currentPage = 1;
        loadUsers();
    });

    // 부서 목록 로드
    async function loadDepartments() {
        try {
            const response = await fetch('/api/departments');
            if (!response.ok) throw new Error('부서 목록을 불러오는데 실패했습니다.');
            
            const departments = await response.json();
            const departmentSelects = document.querySelectorAll('#department, #modalDepartment');
            
            departmentSelects.forEach(select => {
                departments.forEach(dept => {
                    const option = document.createElement('option');
                    option.value = dept.id;
                    option.textContent = dept.name;
                    select.appendChild(option);
                });
            });

        } catch (error) {
            console.error('부서 목록 로드 오류:', error);
            showNotification('부서 목록을 불러오는데 실패했습니다.', 'error');
        }
    }

    // 사용자 목록 로드
    async function loadUsers() {
        try {
            const params = new URLSearchParams({
                page: currentPage - 1,
                size: 10,
                department: document.getElementById('department').value,
                role: document.getElementById('role').value,
                search: document.getElementById('search').value
            });

            const response = await fetch(`/api/users?${params}`);
            if (!response.ok) throw new Error('사용자 목록을 불러오는데 실패했습니다.');
            
            const data = await response.json();
            renderUsers(data.content);
            renderPagination(data.totalPages);

        } catch (error) {
            console.error('사용자 목록 로드 오류:', error);
            showNotification('사용자 목록을 불러오는데 실패했습니다.', 'error');
        }
    }

    // 사용자 목록 렌더링
    function renderUsers(users) {
        userList.innerHTML = '';
        
        users.forEach(user => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${user.employeeId}</td>
                <td>${user.name}</td>
                <td>${user.department}</td>
                <td>${user.position}</td>
                <td>${user.email}</td>
                <td><span class="badge ${getRoleBadgeClass(user.role)}">${getRoleText(user.role)}</span></td>
                <td><span class="badge ${getStatusBadgeClass(user.status)}">${getStatusText(user.status)}</span></td>
                <td>${formatDate(user.lastLoginAt)}</td>
                <td>
                    <div class="btn-group">
                        <button type="button" class="btn btn-sm btn-outline-primary btn-edit" data-id="${user.id}">
                            <i class="mdi mdi-pencil"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-danger btn-delete" data-id="${user.id}">
                            <i class="mdi mdi-delete"></i>
                        </button>
                    </div>
                </td>
            `;

            // 수정 버튼 이벤트
            row.querySelector('.btn-edit').addEventListener('click', () => showEditModal(user));

            // 삭제 버튼 이벤트
            row.querySelector('.btn-delete').addEventListener('click', () => showDeleteConfirm(user.id));

            userList.appendChild(row);
        });

        if (users.length === 0) {
            userList.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center py-5">
                        <i class="mdi mdi-account-off" style="font-size: 48px; color: #ccc;"></i>
                        <p class="mt-3 text-muted">등록된 사용자가 없습니다.</p>
                    </td>
                </tr>
            `;
        }
    }

    // 페이지네이션 렌더링
    function renderPagination(total) {
        totalPages = total;
        const items = [];
        
        // 이전 버튼
        items.push(`
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage - 1}">이전</a>
            </li>
        `);

        // 페이지 번호
        for (let i = 1; i <= total; i++) {
            items.push(`
                <li class="page-item ${currentPage === i ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }

        // 다음 버튼
        items.push(`
            <li class="page-item ${currentPage === total ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage + 1}">다음</a>
            </li>
        `);

        pagination.innerHTML = items.join('');

        // 페이지 클릭 이벤트
        pagination.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const page = parseInt(this.dataset.page);
                if (page > 0 && page <= totalPages) {
                    currentPage = page;
                    loadUsers();
                }
            });
        });
    }

    // 사용자 추가/수정 모달 표시
    function showEditModal(user = null) {
        const form = document.getElementById('userForm');
        const modalTitle = document.getElementById('modalTitle');
        
        // 폼 초기화
        form.reset();
        
        if (user) {
            modalTitle.textContent = '사용자 수정';
            document.getElementById('userId').value = user.id;
            document.getElementById('employeeId').value = user.employeeId;
            document.getElementById('name').value = user.name;
            document.getElementById('modalDepartment').value = user.departmentId;
            document.getElementById('position').value = user.position;
            document.getElementById('email').value = user.email;
            document.getElementById('phone').value = user.phone;
            document.getElementById('modalRole').value = user.role;
            document.getElementById('status').value = user.status;
            document.getElementById('password').required = false;
        } else {
            modalTitle.textContent = '사용자 추가';
            document.getElementById('userId').value = '';
            document.getElementById('password').required = true;
        }

        userModal.show();
    }

    // 사용자 저장
    document.getElementById('saveUser').addEventListener('click', async function() {
        const form = document.getElementById('userForm');
        
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }

        const userId = document.getElementById('userId').value;
        const userData = {
            employeeId: document.getElementById('employeeId').value,
            name: document.getElementById('name').value,
            departmentId: document.getElementById('modalDepartment').value,
            position: document.getElementById('position').value,
            email: document.getElementById('email').value,
            phone: document.getElementById('phone').value,
            role: document.getElementById('modalRole').value,
            status: document.getElementById('status').value
        };

        const password = document.getElementById('password').value;
        if (password) {
            userData.password = password;
        }

        try {
            const response = await fetch(`/api/users${userId ? `/${userId}` : ''}`, {
                method: userId ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });

            if (!response.ok) throw new Error('사용자 저장에 실패했습니다.');

            userModal.hide();
            showNotification('사용자가 저장되었습니다.', 'success');
            loadUsers();

        } catch (error) {
            console.error('사용자 저장 오류:', error);
            showNotification('사용자 저장에 실패했습니다.', 'error');
        }
    });

    // 삭제 확인 모달 표시
    function showDeleteConfirm(id) {
        selectedUserId = id;
        deleteModal.show();
    }

    // 사용자 삭제
    document.getElementById('confirmDelete').addEventListener('click', async function() {
        if (!selectedUserId) return;

        try {
            const response = await fetch(`/api/users/${selectedUserId}`, {
                method: 'DELETE'
            });

            if (!response.ok) throw new Error('사용자 삭제에 실패했습니다.');

            deleteModal.hide();
            showNotification('사용자가 삭제되었습니다.', 'success');
            loadUsers();

        } catch (error) {
            console.error('사용자 삭제 오류:', error);
            showNotification('사용자 삭제에 실패했습니다.', 'error');
        }
    });

    // 권한 뱃지 클래스
    function getRoleBadgeClass(role) {
        const classes = {
            'ADMIN': 'bg-danger',
            'MANAGER': 'bg-warning',
            'USER': 'bg-primary'
        };
        return classes[role] || 'bg-secondary';
    }

    // 상태 뱃지 클래스
    function getStatusBadgeClass(status) {
        const classes = {
            'ACTIVE': 'bg-success',
            'INACTIVE': 'bg-secondary',
            'SUSPENDED': 'bg-danger'
        };
        return classes[status] || 'bg-secondary';
    }

    // 권한 텍스트
    function getRoleText(role) {
        const texts = {
            'ADMIN': '관리자',
            'MANAGER': '매니저',
            'USER': '일반 사용자'
        };
        return texts[role] || role;
    }

    // 상태 텍스트
    function getStatusText(status) {
        const texts = {
            'ACTIVE': '활성',
            'INACTIVE': '비활성',
            'SUSPENDED': '정지'
        };
        return texts[status] || status;
    }

    // 날짜 포맷
    function formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}); 