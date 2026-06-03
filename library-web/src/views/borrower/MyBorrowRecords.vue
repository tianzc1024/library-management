<template>
  <div class="my-records-container">
    <div class="page-title">
      <h2>我的借阅记录</h2>
      <p class="welcome-text">欢迎，{{ borrowerName }}（{{ phone }}）</p>
    </div>

    <el-card>
      <div slot="header">
        <span>借阅记录</span>
        <el-button type="text" style="float: right" icon="el-icon-refresh" @click="loadRecords">
          刷新
        </el-button>
      </div>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="bookName" label="书名" min-width="160" />
        <el-table-column prop="bookAuthor" label="作者" width="140" />
        <el-table-column prop="bookIsbn" label="ISBN" width="160" />
        <el-table-column prop="borrowDate" label="借阅日期" width="180">
          <template slot-scope="scope">
            {{ formatDate(scope.row.borrowDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应还日期" width="180">
          <template slot-scope="scope">
            {{ formatDate(scope.row.dueDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="returnDate" label="归还日期" width="180">
          <template slot-scope="scope">
            {{ formatDate(scope.row.returnDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 0" type="warning" size="small">借阅中</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success" size="small">已归还</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="danger" size="small">已逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.status === 0">
              <span v-if="scope.row.remainingDays >= 0" style="color: #67c23a">
                {{ scope.row.remainingDays }} 天
              </span>
              <span v-else style="color: #f56c6c">
                逾期 {{ Math.abs(scope.row.remainingDays) }} 天
              </span>
            </span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="records.length === 0 && !loading" class="empty-hint">
        暂无借阅记录
      </div>
    </el-card>
  </div>
</template>

<script>
import { getMyRecords } from '@/api/borrower'

export default {
  name: 'MyBorrowRecords',
  data() {
    return {
      records: [],
      loading: false,
      phone: localStorage.getItem('phone') || '',
      borrowerName: localStorage.getItem('name') || ''
    }
  },
  created() {
    this.loadRecords()
  },
  methods: {
    async loadRecords() {
      this.loading = true
      try {
        const res = await getMyRecords()
        this.records = res.data.data || []
      } catch (e) {
        this.$message.error('加载借阅记录失败')
      } finally {
        this.loading = false
      }
    },
    formatDate(dateStr) {
      if (!dateStr) return '-'
      return dateStr.replace('T', ' ').substring(0, 19)
    }
  }
}
</script>

<style scoped>
.my-records-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  text-align: center;
  margin-bottom: 24px;
}

.page-title h2 {
  color: #303133;
  font-size: 24px;
  margin-bottom: 8px;
}

.welcome-text {
  color: #909399;
  font-size: 14px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}
</style>
