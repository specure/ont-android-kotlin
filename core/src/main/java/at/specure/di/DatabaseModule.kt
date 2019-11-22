/*
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.di

import android.content.Context
import androidx.room.Room
import at.specure.database.CoreDatabase
import at.specure.repository.TestDataRepository
import at.specure.repository.TestDataRepositoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger databse module that provides database instance used by core library
 */
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideCoreDatabase(context: Context): CoreDatabase {
        val builder = Room.databaseBuilder(context, CoreDatabase::class.java, "CoreDatabase.db")
        builder.fallbackToDestructiveMigration()
        return builder.build()
    }

    @Provides
    fun provideTestDataRepository(database: CoreDatabase): TestDataRepository = TestDataRepositoryImpl(database)
}
