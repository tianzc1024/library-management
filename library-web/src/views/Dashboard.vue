<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div class="stat-card">
            <div class="stat-num">{{ stats.totalBooks }}</div>
            <div class="stat-label">图书总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-card">
            <div class="stat-num">{{ stats.totalUsers }}</div>
            <div class="stat-label">借阅人员</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-card">
            <div class="stat-num" style="color: #f56c6c">{{ stats.borrowingCount }}</div>
            <div class="stat-label">借阅中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <div slot="header">
        <span style="font-size: 16px; font-weight: bold">
          <i class="el-icon-bell" style="color: #e6a23c"></i>
          归还时间提醒
        </span>
        <el-button size="small" type="primary" style="float: right" @click="loadReminders">刷新</el-button>
      </div>

      <el-alert
        v-if="reminders.length === 0"
        title="当前没有需要提醒的借阅记录"
        type="success"
        :closable="false"
        show-icon
      />

      <el-table v-else :data="reminders" stripe style="width: 100%">
        <el-table-column prop="userName" label="借阅人" width="120" />
        <el-table-column prop="bookName" label="书名" min-width="180" />
        <el-table-column prop="bookAuthor" label="作者" width="120" />
        <el-table-column prop="borrowDate" label="借阅日期" width="160">
          <template slot-scope="scope">
            {{ formatDate(scope.row.borrowDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应还日期" width="160">
          <template slot-scope="scope">
            {{ formatDate(scope.row.dueDate) }}
          </template>
        </el-table-column>
        <el-table-column label="剩余/逾期天数" width="140">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.remainingDays < 0" type="danger" effect="dark">
              已逾期 {{ Math.abs(scope.row.remainingDays) }} 天
            </el-tag>
            <el-tag v-else-if="scope.row.remainingDays === 0" type="warning" effect="dark">
              今天到期
            </el-tag>
            <el-tag v-else type="warning">
              剩余 {{ scope.row.remainingDays }} 天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleReturn(scope.row)">归还</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getReminders, returnBook, getBorrowStats } from '@/api/borrow'
import { getBookList } from '@/api/book'
import { getUserList } from '@/api/user'

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {
        totalBooks: 0,
        totalUsers: 0,
        borrowingCount: 0
      },
      reminders: []
    }
  },
  created() {
    this.loadStats()
    this.loadReminders()
  },
  methods: {
    async loadStats() {
      try {
        const [bookRes, userRes, statsRes] = await Promise.all([
          getBookList(),
          getUserList(),
          getBorrowStats()
        ])
        this.stats.totalBooks = (bookRes.data.data || []).length
        this.stats.totalUsers = (userRes.data.data || []).length
        const statsData = statsRes.data.data || {}
        this.stats.borrowingCount = statsData.borrowingCount || 0
      } catch (e) {
        console.error('加载统计数据失败', e)
      }
    },
    async loadReminders() {
      try {
        const res = await getReminders()
        this.reminders = res.data.data || []
      } catch (e) {
        console.error('加载提醒数据失败', e)
      }
    },
    async handleReturn(row) {
      try {
        await this.$confirm(`确认归还「${row.bookName}」？`, '归还确认', {
          type: 'warning'
        })
        await returnBook(row.recordId)
        this.$message.success('归还成功')
        this.loadReminders()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('归还失败')
        }
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
.stat-card {
  text-align: center;
  padding: 10px 0;
}
.stat-num {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
</style>
