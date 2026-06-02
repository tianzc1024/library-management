<template>
  <div>
    <el-row :gutter="20">
      <!-- 借书 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span style="font-weight: bold">借阅图书</span>
          </div>
          <el-form :model="borrowForm" label-width="100px">
            <el-form-item label="选择借阅人">
              <el-select v-model="borrowForm.userId" placeholder="请选择借阅人" filterable style="width: 100%">
                <el-option
                  v-for="u in userList"
                  :key="u.id"
                  :label="u.name + ' (' + u.phone + ')'"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="选择图书">
              <el-select v-model="borrowForm.bookId" placeholder="请选择图书" filterable style="width: 100%">
                <el-option
                  v-for="b in availableBooks"
                  :key="b.id"
                  :label="b.name + ' - ' + b.author + ' (可借:' + b.availableCount + ')'"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="借阅天数">
              <el-input-number v-model="borrowForm.borrowDays" :min="1" :max="365" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleBorrow">确认借阅</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 还书 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span style="font-weight: bold">归还图书</span>
          </div>
          <el-table :data="borrowingList" stripe max-height="400">
            <el-table-column prop="userName" label="借阅人" width="80" />
            <el-table-column prop="bookName" label="书名" min-width="120" />
            <el-table-column label="应还日期" width="140">
              <template slot-scope="scope">
                {{ formatDate(scope.row.dueDate) }}
              </template>
            </el-table-column>
            <el-table-column label="剩余天数" width="90">
              <template slot-scope="scope">
                <span v-if="scope.row.remainingDays < 0" style="color: #f56c6c">
                  逾期{{ Math.abs(scope.row.remainingDays) }}天
                </span>
                <span v-else style="color: #e6a23c">
                  剩余{{ scope.row.remainingDays }}天
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template slot-scope="scope">
                <el-button size="mini" type="success" @click="handleReturn(scope.row)">归还</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="borrowingList.length === 0" style="text-align: center; color: #909399; padding: 20px">
            暂无借阅中的记录
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getUserList } from '@/api/user'
import { getBookList } from '@/api/book'
import { getBorrowList, borrowBook, returnBook } from '@/api/borrow'

export default {
  name: 'BorrowForm',
  data() {
    return {
      userList: [],
      bookList: [],
      allBorrowList: [],
      borrowForm: {
        userId: null,
        bookId: null,
        borrowDays: 30
      }
    }
  },
  computed: {
    availableBooks() {
      return this.bookList.filter(b => b.availableCount > 0)
    },
    borrowingList() {
      return this.allBorrowList.filter(r => r.status === 0 || r.status === 2)
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const [userRes, bookRes, borrowRes] = await Promise.all([
          getUserList(),
          getBookList(),
          getBorrowList()
        ])
        this.userList = userRes.data.data || []
        this.bookList = bookRes.data.data || []
        this.allBorrowList = borrowRes.data.data || []
      } catch (e) {
        console.error('加载数据失败', e)
      }
    },
    async handleBorrow() {
      if (!this.borrowForm.userId || !this.borrowForm.bookId) {
        this.$message.warning('请选择借阅人和图书')
        return
      }
      try {
        await borrowBook(this.borrowForm)
        this.$message.success('借阅成功')
        this.borrowForm.userId = null
        this.borrowForm.bookId = null
        this.loadData()
      } catch (e) {
        this.$message.error('借阅失败: ' + (e.response?.data?.message || e.message))
      }
    },
    async handleReturn(row) {
      try {
        await this.$confirm(`确认归还「${row.bookName}」？`, '归还确认', { type: 'warning' })
        await returnBook(row.recordId)
        this.$message.success('归还成功')
        this.loadData()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('归还失败')
        }
      }
    },
    formatDate(dateStr) {
      if (!dateStr) return '-'
      return dateStr.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>
