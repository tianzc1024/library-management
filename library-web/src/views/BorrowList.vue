<template>
  <div>
    <el-card>
      <div slot="header">
        <span>借阅记录</span>
        <el-button size="small" type="primary" style="float: right" @click="loadData">刷新</el-button>
      </div>

      <el-table :data="borrowList" stripe>
        <el-table-column prop="userName" label="借阅人" width="100" />
        <el-table-column prop="bookName" label="书名" min-width="160" />
        <el-table-column prop="bookAuthor" label="作者" width="100" />
        <el-table-column prop="bookIsbn" label="ISBN" width="130" />
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
        <el-table-column prop="returnDate" label="归还日期" width="160">
          <template slot-scope="scope">
            {{ formatDate(scope.row.returnDate) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 0" type="primary">借阅中</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">已归还</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="danger">已逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.status === 0 || scope.row.status === 2">
              <span v-if="scope.row.remainingDays < 0" style="color: #f56c6c">
                逾期 {{ Math.abs(scope.row.remainingDays) }}天
              </span>
              <span v-else style="color: #e6a23c">
                剩余 {{ scope.row.remainingDays }}天
              </span>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getBorrowList } from '@/api/borrow'

export default {
  name: 'BorrowList',
  data() {
    return {
      borrowList: []
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      const res = await getBorrowList()
      this.borrowList = res.data.data || []
    },
    formatDate(dateStr) {
      if (!dateStr) return '-'
      return dateStr.replace('T', ' ').substring(0, 19)
    }
  }
}
</script>
