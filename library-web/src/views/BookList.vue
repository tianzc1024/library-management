<template>
  <div>
    <el-card>
      <div slot="header">
        <span>图书管理</span>
        <el-button type="primary" size="small" style="float: right" @click="openAddDialog">添加图书</el-button>
      </div>

      <el-table :data="bookList" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="书名" min-width="150" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="publisher" label="出版社" width="140" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="totalCount" label="总数量" width="80" />
        <el-table-column prop="availableCount" label="可借数量" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.availableCount > 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.availableCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="书名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input v-model="form.isbn" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="form.publisher" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="总数量">
          <el-input-number v-model="form.totalCount" :min="1" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getBookList, addBook, updateBook, deleteBook } from '@/api/book'

export default {
  name: 'BookList',
  data() {
    return {
      bookList: [],
      dialogVisible: false,
      dialogTitle: '添加图书',
      isEdit: false,
      form: { name: '', author: '', isbn: '', publisher: '', category: '', totalCount: 1 }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      const res = await getBookList()
      this.bookList = res.data.data || []
    },
    openAddDialog() {
      this.dialogTitle = '添加图书'
      this.isEdit = false
      this.form = { name: '', author: '', isbn: '', publisher: '', category: '', totalCount: 1 }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.dialogTitle = '编辑图书'
      this.isEdit = true
      this.form = { ...row }
      this.dialogVisible = true
    },
    async handleSave() {
      try {
        if (this.isEdit) {
          await updateBook(this.form)
        } else {
          await addBook(this.form)
        }
        this.$message.success(this.isEdit ? '更新成功' : '添加成功')
        this.dialogVisible = false
        this.loadData()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async handleDelete(id) {
      try {
        await this.$confirm('确定删除该图书吗？', '提示', { type: 'warning' })
        await deleteBook(id)
        this.$message.success('删除成功')
        this.loadData()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败')
      }
    }
  }
}
</script>
